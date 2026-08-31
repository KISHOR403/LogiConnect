import type { Metadata } from 'next';
import React from 'react';

export const metadata: Metadata = {
  title: 'LogiConnect - Enterprise Collaboration Platform',
  description: 'Internal communication and operations platform for logistics teams.',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
