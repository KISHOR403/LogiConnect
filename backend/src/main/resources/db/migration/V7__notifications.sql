-- ==============================================================================
-- LOGICONNECT ENTERPRISE INTERNAL COMMUNICATION PLATFORM
-- Database Migration: V7__notifications.sql
-- Module: Domain 7 - In-App Notification System & Event Foundation
-- ==============================================================================

CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    reference_type VARCHAR(50),
    reference_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_notifications_type CHECK (type IN (
        'MESSAGE',
        'GROUP_MESSAGE',
        'CHANNEL_MESSAGE',
        'ANNOUNCEMENT',
        'URGENT_ANNOUNCEMENT',
        'ACKNOWLEDGEMENT_REQUIRED',
        'MEETING_INVITATION',
        'MEETING_UPDATED',
        'MEETING_CANCELLED',
        'MENTION',
        'SECURITY'
    )),
    CONSTRAINT chk_notifications_read_state CHECK (
        (is_read = FALSE AND read_at IS NULL) OR
        (is_read = TRUE AND read_at IS NOT NULL)
    )
);

-- Compound Index for paginated user notification feed
CREATE INDEX IF NOT EXISTS idx_notifications_user_feed 
    ON public.notifications (user_id, created_at DESC, id DESC);

-- Compound Index for high-frequency unread badge counter and filtering
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread 
    ON public.notifications (user_id, is_read, created_at DESC);

-- Index for ownership and single notification lookup
CREATE INDEX IF NOT EXISTS idx_notifications_id_user 
    ON public.notifications (id, user_id);

-- Index for duplicate prevention and reference lookups
CREATE INDEX IF NOT EXISTS idx_notifications_ref_lookup 
    ON public.notifications (user_id, reference_type, reference_id, type);
