import React, { useState } from 'react';
import { Search, X, Command } from 'lucide-react';
import { cn } from '@/lib/utils/cn';

export interface SearchBarProps {
  placeholder?: string;
  onSearch?: (query: string) => void;
  className?: string;
  autoFocus?: boolean;
}

export const SearchBar: React.FC<SearchBarProps> = ({
  placeholder = 'Search people, messages, channels, announcements...',
  onSearch,
  className,
  autoFocus = false,
}) => {
  const [query, setQuery] = useState('');

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setQuery(val);
    onSearch?.(val);
  };

  const handleClear = () => {
    setQuery('');
    onSearch?.('');
  };

  return (
    <div className={cn('relative w-full max-w-lg', className)}>
      <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
        <Search size={16} aria-hidden="true" />
      </div>
      <input
        type="search"
        value={query}
        onChange={handleChange}
        autoFocus={autoFocus}
        placeholder={placeholder}
        aria-label="Global search across LogiConnect"
        className={cn(
          'w-full pl-9 pr-16 py-1.5 text-sm bg-slate-100/80 hover:bg-slate-100 text-slate-900 rounded-lg border border-transparent',
          'focus:bg-white focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/20 transition-all placeholder:text-slate-400'
        )}
      />
      <div className="absolute inset-y-0 right-0 pr-2 flex items-center gap-1">
        {query ? (
          <button
            onClick={handleClear}
            className="p-1 text-slate-400 hover:text-slate-600 rounded-md transition-colors focus-ring"
            aria-label="Clear search query"
          >
            <X size={14} />
          </button>
        ) : (
          <kbd className="hidden sm:inline-flex items-center gap-0.5 px-1.5 py-0.5 text-[10px] font-mono text-slate-400 bg-slate-200/60 rounded border border-slate-300/60 select-none">
            <Command size={10} /> K
          </kbd>
        )}
      </div>
    </div>
  );
};
