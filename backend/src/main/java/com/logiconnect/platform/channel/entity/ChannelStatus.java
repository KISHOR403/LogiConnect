package com.logiconnect.platform.channel.entity;

/**
 * Channel lifecycle status matching the database check constraint:
 * 'ACTIVE', 'ARCHIVED', 'DELETED'
 */
public enum ChannelStatus {
    ACTIVE,
    ARCHIVED,
    DELETED
}
