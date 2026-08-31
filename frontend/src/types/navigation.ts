import { Role } from './auth';
import React from 'react';

export interface NavItem {
  id: string;
  label: string;
  path: string;
  icon: React.ComponentType<{ className?: string; size?: number | string }>;
  badge?: number | string;
  roles?: Role[];
  exact?: boolean;
}

export interface NavSection {
  id: string;
  title?: string;
  items: NavItem[];
  roles?: Role[];
}

export interface BreadcrumbItem {
  label: string;
  path?: string;
}
