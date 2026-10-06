-- Migration: 20261006000000_fix_addresses_user_id_default.sql
-- Description: Set default auth.uid() on public.addresses.user_id to derive identity at trust boundary

ALTER TABLE public.addresses
ALTER COLUMN user_id SET DEFAULT auth.uid();
