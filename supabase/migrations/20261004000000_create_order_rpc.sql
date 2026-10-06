-- Migration: 20261004000000_create_order_rpc.sql
-- Description: Phase 4 Authoritative Atomic Order Creation RPC and Idempotency Schema
-- Authority: 01_PRODUCT_SPEC.md (§7, §10), 03_TECH_ARCHITECTURE.md (§10), 04_DATA_MODEL.md (§12, §13, §14), 13_API_CONTRACT.md (§19, §20, §21)

-- 0. Ensure system_configurations table exists for authoritative backend settings
CREATE TABLE IF NOT EXISTS public.system_configurations (
    key TEXT PRIMARY KEY,
    value JSONB NOT NULL,
    description TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.system_configurations ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
    DROP POLICY IF EXISTS "Public read system configurations" ON public.system_configurations;
    CREATE POLICY "Public read system configurations" ON public.system_configurations
        FOR SELECT TO authenticated, anon
        USING (true);
END $$;

-- Table comments and contract schema: Authoritative configuration rows (e.g., 'delivery_fee_config')
-- MUST be provisioned at runtime by backend operations and MUST NOT embed any hardcoded monetary defaults in source code.
COMMENT ON TABLE public.system_configurations IS 'Authoritative backend system configuration parameters.';
COMMENT ON COLUMN public.system_configurations.key IS 'Configuration key name, e.g., delivery_fee_config.';
COMMENT ON COLUMN public.system_configurations.value IS 'JSONB configuration payload. For delivery_fee_config, schema is {"default_fee": <NUMERIC>, "currency": "EGP", "active": <BOOLEAN>}. Required at runtime; fails safely if missing, inactive, or non-numeric.';

-- 1. Ensure addresses table exists with strict RLS
CREATE TABLE IF NOT EXISTS public.addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    label TEXT NOT NULL DEFAULT 'Home',
    governorate TEXT NOT NULL DEFAULT 'Cairo',
    city TEXT NOT NULL,
    area TEXT NOT NULL,
    street TEXT NOT NULL,
    building TEXT NOT NULL,
    apartment TEXT,
    floor TEXT,
    landmark TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.addresses ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
    DROP POLICY IF EXISTS "Users can read own addresses" ON public.addresses;
    CREATE POLICY "Users can read own addresses" ON public.addresses
        FOR SELECT TO authenticated
        USING (auth.uid() = user_id);

    DROP POLICY IF EXISTS "Users can insert own addresses" ON public.addresses;
    CREATE POLICY "Users can insert own addresses" ON public.addresses
        FOR INSERT TO authenticated
        WITH CHECK (auth.uid() = user_id);

    DROP POLICY IF EXISTS "Users can update own addresses" ON public.addresses;
    CREATE POLICY "Users can update own addresses" ON public.addresses
        FOR UPDATE TO authenticated
        USING (auth.uid() = user_id)
        WITH CHECK (auth.uid() = user_id);
END $$;

-- 2. Ensure orders table exists with strict RLS and idempotency key
CREATE TABLE IF NOT EXISTS public.orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    public_order_number TEXT NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE RESTRICT,
    address_id UUID REFERENCES public.addresses(id) ON DELETE SET NULL,
    delivery_address_snapshot JSONB NOT NULL,
    status TEXT NOT NULL DEFAULT 'PLACED',
    subtotal NUMERIC(12, 2) NOT NULL CHECK (subtotal >= 0),
    delivery_fee NUMERIC(12, 2) NOT NULL CHECK (delivery_fee >= 0),
    discount NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (discount >= 0),
    total NUMERIC(12, 2) NOT NULL CHECK (total >= 0),
    payment_method TEXT NOT NULL DEFAULT 'CASH_ON_DELIVERY',
    payment_status TEXT NOT NULL DEFAULT 'PENDING',
    idempotency_key TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_orders_user_idempotency UNIQUE (user_id, idempotency_key)
);

ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
    DROP POLICY IF EXISTS "Patients can read own orders" ON public.orders;
    CREATE POLICY "Patients can read own orders" ON public.orders
        FOR SELECT TO authenticated
        USING (auth.uid() = user_id);
END $$;

-- 3. Ensure order_items table exists with strict RLS
CREATE TABLE IF NOT EXISTS public.order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
    medicine_variant_id UUID NOT NULL,
    medicine_name_snapshot TEXT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0),
    line_total NUMERIC(12, 2) NOT NULL CHECK (line_total >= 0),
    source_pharmacy_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.order_items ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
    DROP POLICY IF EXISTS "Patients can read own order items" ON public.order_items;
    CREATE POLICY "Patients can read own order items" ON public.order_items
        FOR SELECT TO authenticated
        USING (EXISTS (
            SELECT 1 FROM public.orders o
            WHERE o.id = order_items.order_id
            AND o.user_id = auth.uid()
        ));
END $$;

-- 4. Ensure order_status_history table exists with strict RLS
CREATE TABLE IF NOT EXISTS public.order_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
    from_status TEXT,
    to_status TEXT NOT NULL,
    actor_type TEXT NOT NULL DEFAULT 'PATIENT',
    actor_id UUID NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.order_status_history ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
    DROP POLICY IF EXISTS "Patients can read own order history" ON public.order_status_history;
    CREATE POLICY "Patients can read own order history" ON public.order_status_history
        FOR SELECT TO authenticated
        USING (EXISTS (
            SELECT 1 FROM public.orders o
            WHERE o.id = order_status_history.order_id
            AND o.user_id = auth.uid()
        ));
END $$;

-- 5. Atomic Order Creation Function (create_order)
-- Guaranteed atomic transaction: locks inventory, validates stock & active pharmacy,
-- computes authoritative line items, subtotal, delivery fee, and grand total,
-- stores immutable address snapshot, creates order and order_items, increments reserved_quantity,
-- logs status history, and returns authoritative order object.
-- Rolls back on ANY validation or constraint error.
CREATE OR REPLACE FUNCTION public.create_order(
    p_address_id UUID,
    p_pharmacy_id UUID,
    p_items JSONB,
    p_payment_method TEXT DEFAULT 'CASH_ON_DELIVERY',
    p_idempotency_key TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_user_id UUID;
    v_existing_order JSONB;
    v_config RECORD;
    v_address RECORD;
    v_address_snapshot JSONB;
    v_pharmacy RECORD;
    v_item JSONB;
    v_variant_id UUID;
    v_req_qty INT;
    v_inv RECORD;
    v_med_name TEXT;
    v_line_total NUMERIC(12, 2);
    v_subtotal NUMERIC(12, 2) := 0.00;
    v_delivery_fee NUMERIC(12, 2); -- Authoritative backend configuration source
    v_discount NUMERIC(12, 2) := 0.00;
    v_total NUMERIC(12, 2);
    v_order_id UUID;
    v_public_order_number TEXT;
    v_order_record RECORD;
    v_created_at TIMESTAMPTZ;
BEGIN
    -- 1. Derive authenticated patient identity from JWT context
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'AUTH_REQUIRED: Authentication is required to place an order.'
            USING ERRCODE = '28000';
    END IF;

    -- Validate idempotency key presence
    IF p_idempotency_key IS NULL OR trim(p_idempotency_key) = '' THEN
        RAISE EXCEPTION 'IDEMPOTENCY_KEY_REQUIRED: An idempotency key must be provided.'
            USING ERRCODE = '22023';
    END IF;

    -- 2. Concurrency Serialization via Transaction-Level Advisory Lock:
    -- Serializes concurrent requests for the exact same (user_id, idempotency_key).
    -- Released automatically when transaction commits or rolls back.
    -- Prevents concurrent race conditions from duplicating inventory reservation or orders.
    PERFORM pg_advisory_xact_lock(hashtext(v_user_id::text || ':' || p_idempotency_key));

    -- 3. Idempotency Check: Return existing order if same key already submitted by user
    SELECT jsonb_build_object(
        'id', id,
        'public_order_number', public_order_number,
        'status', status,
        'subtotal', subtotal,
        'delivery_fee', delivery_fee,
        'discount', discount,
        'total', total,
        'payment_method', payment_method,
        'payment_status', payment_status,
        'created_at', created_at
    ) INTO v_existing_order
    FROM public.orders
    WHERE user_id = v_user_id AND idempotency_key = p_idempotency_key;

    IF v_existing_order IS NOT NULL THEN
        RETURN v_existing_order;
    END IF;

    -- 4. Authoritative Delivery Fee Configuration Retrieval:
    -- Read from backend system_configurations; strictly validated at runtime.
    -- Fails safely if missing, inactive, or non-numeric. Does not fall back to any hardcoded constant.
    SELECT * INTO v_config
    FROM public.system_configurations
    WHERE key = 'delivery_fee_config';

    IF NOT FOUND THEN
        RAISE EXCEPTION 'CONFIG_ERROR: Authoritative delivery fee configuration not found.'
            USING ERRCODE = '55000';
    END IF;

    IF COALESCE((v_config.value->>'active')::BOOLEAN, false) IS NOT TRUE THEN
        RAISE EXCEPTION 'CONFIG_ERROR: Authoritative delivery fee configuration is inactive.'
            USING ERRCODE = '55000';
    END IF;

    BEGIN
        v_delivery_fee := COALESCE(v_config.value->>'default_fee', v_config.value->>'fee')::NUMERIC(12, 2);
    EXCEPTION WHEN OTHERS THEN
        RAISE EXCEPTION 'CONFIG_ERROR: Authoritative delivery fee cannot be interpreted as a valid monetary amount.'
            USING ERRCODE = '55000';
    END;

    IF v_delivery_fee IS NULL OR v_delivery_fee < 0 THEN
        RAISE EXCEPTION 'CONFIG_ERROR: Authoritative delivery fee must be a valid non-negative monetary amount.'
            USING ERRCODE = '55000';
    END IF;

    -- 5. Validate payment method (strictly CASH_ON_DELIVERY for Phase 4)
    IF p_payment_method IS NULL OR p_payment_method <> 'CASH_ON_DELIVERY' THEN
        RAISE EXCEPTION 'INVALID_PAYMENT_METHOD: Only CASH_ON_DELIVERY is accepted.'
            USING ERRCODE = '22023';
    END IF;

    -- 6. Validate Delivery Address existence and ownership
    SELECT * INTO v_address
    FROM public.addresses
    WHERE id = p_address_id AND user_id = v_user_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'ADDRESS_NOT_FOUND: Delivery address does not exist or does not belong to the authenticated user.'
            USING ERRCODE = '02000';
    END IF;

    v_address_snapshot := jsonb_build_object(
        'address_id', v_address.id,
        'label', v_address.label,
        'governorate', v_address.governorate,
        'city', v_address.city,
        'area', v_address.area,
        'street', v_address.street,
        'building', v_address.building,
        'apartment', v_address.apartment,
        'floor', v_address.floor,
        'landmark', v_address.landmark
    );

    -- 7. Validate Selected Pharmacy status AND verification status
    SELECT * INTO v_pharmacy
    FROM public.pharmacies
    WHERE id = p_pharmacy_id
      AND status = 'ACTIVE'
      AND verification_status = 'VERIFIED';

    IF NOT FOUND THEN
        RAISE EXCEPTION 'PHARMACY_UNAVAILABLE: Selected partner pharmacy is not active or verified.'
            USING ERRCODE = '02000';
    END IF;

    -- 6. Validate items array
    IF p_items IS NULL OR jsonb_array_length(p_items) = 0 THEN
        RAISE EXCEPTION 'CART_EMPTY: Cannot place an order with an empty items list.'
            USING ERRCODE = '22023';
    END IF;

    -- 7. Validate inventory and calculate line totals
    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items)
    LOOP
        v_variant_id := (v_item->>'variant_id')::UUID;
        v_req_qty := (v_item->>'quantity')::INT;

        IF v_req_qty IS NULL OR v_req_qty <= 0 THEN
            RAISE EXCEPTION 'INVALID_QUANTITY: Requested quantity must be greater than zero.'
                USING ERRCODE = '22023';
        END IF;

        -- Lock inventory row for update
        SELECT pi.id, pi.price, pi.quantity, COALESCE(pi.reserved_quantity, 0) AS reserved_quantity,
               pi.availability_status, m.name_ar, mv.brand_name
        INTO v_inv
        FROM public.pharmacy_inventory pi
        JOIN public.medicine_variants mv ON pi.medicine_variant_id = mv.id
        JOIN public.medicines m ON mv.medicine_id = m.id
        WHERE pi.pharmacy_id = p_pharmacy_id
          AND pi.medicine_variant_id = v_variant_id
        FOR UPDATE;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'ITEM_UNAVAILABLE: Medicine variant % is not available at pharmacy %.', v_variant_id, p_pharmacy_id
                USING ERRCODE = '02000';
        END IF;

        IF v_inv.availability_status NOT IN ('AVAILABLE', 'LOW_STOCK') THEN
            RAISE EXCEPTION 'OUT_OF_STOCK: Medicine % is currently out of stock.', v_inv.name_ar
                USING ERRCODE = '55000';
        END IF;

        IF (v_inv.quantity - v_inv.reserved_quantity) < v_req_qty THEN
            RAISE EXCEPTION 'INSUFFICIENT_STOCK: Requested quantity (% units) exceeds available stock (% units) for %.',
                v_req_qty, (v_inv.quantity - v_inv.reserved_quantity), v_inv.name_ar
                USING ERRCODE = '55000';
        END IF;

        v_line_total := round(v_inv.price * v_req_qty, 2);
        v_subtotal := v_subtotal + v_line_total;
    END LOOP;

    -- 8. Compute grand total
    v_total := round(v_subtotal + v_delivery_fee - v_discount, 2);
    v_order_id := gen_random_uuid();
    v_public_order_number := 'ELAJX-' || upper(substr(md5(random()::text || clock_timestamp()::text), 1, 8));
    v_created_at := clock_timestamp();

    -- 9. Insert into orders table
    INSERT INTO public.orders (
        id,
        public_order_number,
        user_id,
        address_id,
        delivery_address_snapshot,
        status,
        subtotal,
        delivery_fee,
        discount,
        total,
        payment_method,
        payment_status,
        idempotency_key,
        created_at,
        updated_at
    ) VALUES (
        v_order_id,
        v_public_order_number,
        v_user_id,
        v_address.id,
        v_address_snapshot,
        'PLACED',
        v_subtotal,
        v_delivery_fee,
        v_discount,
        v_total,
        'CASH_ON_DELIVERY',
        'PENDING',
        p_idempotency_key,
        v_created_at,
        v_created_at
    );

    -- 10. Insert order items and reserve inventory
    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items)
    LOOP
        v_variant_id := (v_item->>'variant_id')::UUID;
        v_req_qty := (v_item->>'quantity')::INT;

        SELECT pi.id, pi.price, m.name_ar, mv.brand_name
        INTO v_inv
        FROM public.pharmacy_inventory pi
        JOIN public.medicine_variants mv ON pi.medicine_variant_id = mv.id
        JOIN public.medicines m ON mv.medicine_id = m.id
        WHERE pi.pharmacy_id = p_pharmacy_id
          AND pi.medicine_variant_id = v_variant_id;

        v_med_name := COALESCE(v_inv.name_ar, v_inv.brand_name, 'Medication');
        v_line_total := round(v_inv.price * v_req_qty, 2);

        INSERT INTO public.order_items (
            id,
            order_id,
            medicine_variant_id,
            medicine_name_snapshot,
            quantity,
            unit_price,
            line_total,
            source_pharmacy_id,
            created_at
        ) VALUES (
            gen_random_uuid(),
            v_order_id,
            v_variant_id,
            v_med_name,
            v_req_qty,
            v_inv.price,
            v_line_total,
            p_pharmacy_id,
            v_created_at
        );

        -- Atomically increment reserved_quantity
        UPDATE public.pharmacy_inventory
        SET reserved_quantity = COALESCE(reserved_quantity, 0) + v_req_qty,
            updated_at = v_created_at
        WHERE id = v_inv.id;
    END LOOP;

    -- 11. Record initial status history
    INSERT INTO public.order_status_history (
        id,
        order_id,
        from_status,
        to_status,
        actor_type,
        actor_id,
        note,
        created_at
    ) VALUES (
        gen_random_uuid(),
        v_order_id,
        NULL,
        'PLACED',
        'PATIENT',
        v_user_id,
        'Order submitted by patient with Cash on Delivery',
        v_created_at
    );

    -- 12. Return authoritative created order payload
    RETURN jsonb_build_object(
        'id', v_order_id,
        'public_order_number', v_public_order_number,
        'status', 'PLACED',
        'subtotal', v_subtotal,
        'delivery_fee', v_delivery_fee,
        'discount', v_discount,
        'total', v_total,
        'payment_method', 'CASH_ON_DELIVERY',
        'payment_status', 'PENDING',
        'created_at', v_created_at
    );
END;
$$;
