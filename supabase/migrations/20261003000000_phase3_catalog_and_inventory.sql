-- Migration: 20261003000000_phase3_catalog_and_inventory.sql
-- Description: Non-destructive Phase 3 schema synchronization for ElajX master catalog and inventory
-- Rules: Zero fabricated data, zero fake rows, zero destructive statements, legacy tables preserved.

BEGIN;

-- 1. Create public.pharmacies schema (empty, awaiting authoritative partner pharmacy onboarding)
CREATE TABLE IF NOT EXISTS public.pharmacies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_code TEXT NOT NULL UNIQUE,
    name_private TEXT NOT NULL,
    governorate TEXT NOT NULL DEFAULT 'Cairo',
    city TEXT NOT NULL,
    area TEXT NOT NULL,
    address_line TEXT,
    phone TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    verification_status TEXT NOT NULL DEFAULT 'VERIFIED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 2. Non-destructively evolve public.medicines table
-- Adds missing Phase 3 attributes without destroying or altering existing legacy columns
ALTER TABLE public.medicines ADD COLUMN IF NOT EXISTS name_ar TEXT;
ALTER TABLE public.medicines ADD COLUMN IF NOT EXISTS name_en TEXT;
ALTER TABLE public.medicines ADD COLUMN IF NOT EXISTS normalized_name TEXT;
ALTER TABLE public.medicines ADD COLUMN IF NOT EXISTS strength TEXT;
ALTER TABLE public.medicines ADD COLUMN IF NOT EXISTS dosage_form TEXT;
ALTER TABLE public.medicines ADD COLUMN IF NOT EXISTS manufacturer TEXT;
ALTER TABLE public.medicines ADD COLUMN IF NOT EXISTS barcode TEXT;
ALTER TABLE public.medicines ADD COLUMN IF NOT EXISTS prescription_required BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE public.medicines ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE public.medicines ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- Update status safely based ONLY on existing verified boolean column is_available
-- Do NOT assume legacy unilingual 'name' is English name_en.
-- Do NOT assume ASCII lowercase of legacy 'name' is compliant normalized_name.
-- Unknown medical attributes (name_ar, name_en, normalized_name, strength, dosage_form, manufacturer, barcode)
-- remain strictly NULL until authoritative catalog data is populated.
UPDATE public.medicines
SET 
    status = 'INACTIVE',
    updated_at = now()
WHERE is_available = false AND status <> 'INACTIVE';

-- 3. Create public.medicine_variants schema (empty, awaiting authoritative SKU specifications)
-- No fabricated variants (e.g. 'Standard Pack') are automatically created.
CREATE TABLE IF NOT EXISTS public.medicine_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    medicine_id UUID NOT NULL REFERENCES public.medicines(id) ON DELETE CASCADE,
    brand_name TEXT NOT NULL,
    package_size TEXT NOT NULL,
    barcode TEXT,
    price_reference NUMERIC(12, 2),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 4. Create public.pharmacy_inventory schema (empty, awaiting authoritative inventory and pricing sync)
-- Selling price must be strictly non-negative; NO default 0.00 is used so fake prices cannot enter production.
CREATE TABLE IF NOT EXISTS public.pharmacy_inventory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pharmacy_id UUID NOT NULL REFERENCES public.pharmacies(id) ON DELETE CASCADE,
    medicine_variant_id UUID NOT NULL REFERENCES public.medicine_variants(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    availability_status TEXT NOT NULL DEFAULT 'AVAILABLE',
    price NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    last_synced_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_pharmacy_variant UNIQUE (pharmacy_id, medicine_variant_id)
);

-- Note on legacy pharmacies_stock:
-- public.pharmacies_stock is 100% PRESERVED and untouched. No unverified data is migrated automatically.

-- 5. Performance and Search Indexes
-- Required composite index for client alternatives discovery
CREATE INDEX IF NOT EXISTS idx_medicines_active_ingredient_strength_dosage 
ON public.medicines (active_ingredient, strength, dosage_form);

CREATE INDEX IF NOT EXISTS idx_medicines_status ON public.medicines (status);
CREATE INDEX IF NOT EXISTS idx_medicine_variants_medicine_id ON public.medicine_variants (medicine_id);
CREATE INDEX IF NOT EXISTS idx_medicine_variants_active ON public.medicine_variants (active);
CREATE INDEX IF NOT EXISTS idx_pharmacies_display_code ON public.pharmacies (display_code);
CREATE INDEX IF NOT EXISTS idx_pharmacies_status_verification ON public.pharmacies (status, verification_status);
CREATE INDEX IF NOT EXISTS idx_pharmacy_inventory_variant ON public.pharmacy_inventory (medicine_variant_id);
CREATE INDEX IF NOT EXISTS idx_pharmacy_inventory_pharmacy ON public.pharmacy_inventory (pharmacy_id);
CREATE INDEX IF NOT EXISTS idx_pharmacy_inventory_price ON public.pharmacy_inventory (price);

-- 6. Row Level Security (RLS) Configuration
ALTER TABLE public.medicines ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.medicine_variants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pharmacies ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pharmacy_inventory ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
    DROP POLICY IF EXISTS "Public read active medicines" ON public.medicines;
    CREATE POLICY "Public read active medicines" ON public.medicines
        FOR SELECT TO authenticated, anon
        USING (status = 'ACTIVE');

    DROP POLICY IF EXISTS "Public read active variants" ON public.medicine_variants;
    CREATE POLICY "Public read active variants" ON public.medicine_variants
        FOR SELECT TO authenticated, anon
        USING (active = true);

    DROP POLICY IF EXISTS "Public read active verified pharmacies" ON public.pharmacies;
    CREATE POLICY "Public read active verified pharmacies" ON public.pharmacies
        FOR SELECT TO authenticated, anon
        USING (status = 'ACTIVE' AND verification_status = 'VERIFIED');

    DROP POLICY IF EXISTS "Public read pharmacy inventory" ON public.pharmacy_inventory;
    CREATE POLICY "Public read pharmacy inventory" ON public.pharmacy_inventory
        FOR SELECT TO authenticated, anon
        USING (true);
END $$;

COMMIT;
