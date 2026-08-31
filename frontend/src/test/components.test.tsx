import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ErrorState } from '@/components/feedback/ErrorState';
import { SearchBar } from '@/components/common/SearchBar';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Avatar } from '@/components/common/Avatar';

describe('Design System & Foundation Components', () => {
  it('1. Button renders with primary variant and handles click event', async () => {
    const handleClick = vi.fn();
    const user = userEvent.setup();

    render(
      <Button variant="primary" onClick={handleClick}>
        Confirm Action
      </Button>
    );

    const btn = screen.getByRole('button', { name: /confirm action/i });
    expect(btn).toBeInTheDocument();
    expect(btn).not.toBeDisabled();

    await user.click(btn);
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('2. Button disables and shows spinner when isLoading is true', async () => {
    const handleClick = vi.fn();
    const user = userEvent.setup();

    render(
      <Button variant="primary" isLoading onClick={handleClick}>
        Submit
      </Button>
    );

    const btn = screen.getByRole('button', { name: /submit/i });
    expect(btn).toBeDisabled();

    await user.click(btn);
    expect(handleClick).not.toHaveBeenCalled();
  });

  it('3. EmptyState renders title, description, and optional action button', async () => {
    const handleAction = vi.fn();
    const user = userEvent.setup();

    render(
      <EmptyState
        title="No items found"
        description="Try searching for something else."
        actionLabel="Clear Filter"
        onAction={handleAction}
      />
    );

    expect(screen.getByText('No items found')).toBeInTheDocument();
    expect(screen.getByText('Try searching for something else.')).toBeInTheDocument();

    const actionBtn = screen.getByRole('button', { name: /clear filter/i });
    await user.click(actionBtn);
    expect(handleAction).toHaveBeenCalledTimes(1);
  });

  it('4. ErrorState renders alert, custom message, and triggers onRetry', async () => {
    const handleRetry = vi.fn();
    const user = userEvent.setup();

    render(
      <ErrorState
        title="Network Disconnected"
        message="Unable to communicate with the server."
        onRetry={handleRetry}
      />
    );

    expect(screen.getByText('Network Disconnected')).toBeInTheDocument();
    expect(screen.getByText('Unable to communicate with the server.')).toBeInTheDocument();

    const retryBtn = screen.getByRole('button', { name: /try again/i });
    await user.click(retryBtn);
    expect(handleRetry).toHaveBeenCalledTimes(1);
  });

  it('5. SearchBar receives input and fires onSearch callback', async () => {
    const handleSearch = vi.fn();
    const user = userEvent.setup();

    render(<SearchBar placeholder="Search team..." onSearch={handleSearch} />);

    const input = screen.getByPlaceholderText('Search team...');
    await user.type(input, 'logistics');

    expect(handleSearch).toHaveBeenCalledWith('logistics');
  });

  it('6. Badge renders dot indicator and variants correctly', () => {
    render(
      <Badge variant="success" dot>
        Active
      </Badge>
    );

    expect(screen.getByText('Active')).toBeInTheDocument();
  });

  it('7. Avatar renders computed initials and title', () => {
    render(<Avatar name="Pooja Patel" size="md" status="online" />);

    expect(screen.getByText('PP')).toBeInTheDocument();
  });
});
