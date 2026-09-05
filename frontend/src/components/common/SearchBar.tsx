import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Search,
  X,
  Command,
  Users,
  MessageSquare,
  Hash,
  Megaphone,
  Loader2,
  ArrowRight,
} from 'lucide-react';
import { searchService } from '@/services/searchService';
import { GroupedSearchResults, SearchResultItem } from '@/types/search';
import { useOnClickOutside } from '@/hooks/useOnClickOutside';
import { cn } from '@/lib/utils/cn';

export interface SearchBarProps {
  placeholder?: string;
  onSearch?: (query: string) => void;
  className?: string;
  autoFocus?: boolean;
}

const useSafeNavigate = () => {
  try {
    return useNavigate();
  } catch {
    return (path: string) => {
      window.location.href = path;
    };
  }
};

export const SearchBar: React.FC<SearchBarProps> = ({
  placeholder = 'Search people, messages, channels, announcements...',
  onSearch,
  className,
  autoFocus = false,
}) => {
  const [query, setQuery] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [results, setResults] = useState<GroupedSearchResults>({
    people: [],
    channels: [],
    announcements: [],
    messages: [],
    documents: [],
    meetings: [],
  });

  const inputRef = useRef<HTMLInputElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const navigate = useSafeNavigate();

  useOnClickOutside(containerRef, () => setIsOpen(false), isOpen);

  // Global Ctrl+K / Cmd+K listener
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        inputRef.current?.focus();
        setIsOpen(true);
      }
      if (e.key === 'Escape' && isOpen) {
        setIsOpen(false);
        inputRef.current?.blur();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen]);

  // Debounced search query
  useEffect(() => {
    if (!query.trim() || query.trim().length < 2) {
      setResults({
        people: [],
        channels: [],
        announcements: [],
        messages: [],
        documents: [],
        meetings: [],
      });
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    const timer = setTimeout(async () => {
      try {
        const searchResults = await searchService.performGlobalSearch(query);
        setResults(searchResults);
        setIsOpen(true);
      } catch {
        // Silently handle search error
      } finally {
        setIsLoading(false);
      }
    }, 280);

    return () => clearTimeout(timer);
  }, [query]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setQuery(val);
    onSearch?.(val);
    if (!isOpen && val.trim().length >= 2) {
      setIsOpen(true);
    }
  };

  const handleClear = () => {
    setQuery('');
    setIsOpen(false);
    onSearch?.('');
    inputRef.current?.focus();
  };

  const handleSelectResult = (item: SearchResultItem) => {
    setIsOpen(false);
    setQuery('');
    navigate(item.url);
  };

  const totalResultsCount =
    results.people.length +
    results.channels.length +
    results.announcements.length +
    results.messages.length +
    results.documents.length +
    results.meetings.length;

  return (
    <div className={cn('relative w-full max-w-lg', className)} ref={containerRef}>
      <div className="relative flex items-center">
        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
          <Search size={16} aria-hidden="true" />
        </div>
        <input
          ref={inputRef}
          type="search"
          value={query}
          onChange={handleChange}
          onFocus={() => {
            if (query.trim().length >= 2) setIsOpen(true);
          }}
          autoFocus={autoFocus}
          placeholder={placeholder}
          aria-label="Global search across LogiConnect"
          aria-autocomplete="list"
          aria-expanded={isOpen}
          className={cn(
            'w-full pl-9 pr-16 py-1.5 text-sm bg-slate-100/80 hover:bg-slate-100 text-slate-900 rounded-lg border border-transparent',
            'focus:bg-white focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/20 transition-all placeholder:text-slate-400'
          )}
        />
        <div className="absolute inset-y-0 right-0 pr-2 flex items-center gap-1">
          {isLoading ? (
            <Loader2 size={14} className="animate-spin text-brand-600 mr-1" />
          ) : query ? (
            <button
              onClick={handleClear}
              className="p-1 text-slate-400 hover:text-slate-600 rounded-md transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-brand-500"
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

      {/* Dropdown Results Overlay */}
      {isOpen && query.trim().length >= 2 && (
        <div
          className="absolute left-0 right-0 top-full mt-2 bg-white rounded-xl shadow-xl border border-slate-200 py-2 z-50 max-h-[70vh] overflow-y-auto animate-in fade-in slide-in-from-top-1 duration-150"
          role="listbox"
          aria-label="Search suggestions"
        >
          {isLoading && totalResultsCount === 0 ? (
            <div className="py-8 text-center text-xs text-slate-500 flex items-center justify-center gap-2">
              <Loader2 size={15} className="animate-spin text-brand-600" />
              <span>Searching directory, messages, and announcements...</span>
            </div>
          ) : totalResultsCount === 0 ? (
            <div className="py-8 px-4 text-center">
              <p className="text-xs font-semibold text-slate-700">No results found for &ldquo;{query}&rdquo;</p>
              <p className="text-[11px] text-slate-400 mt-0.5">Check for typos or try searching with different keywords.</p>
            </div>
          ) : (
            <div className="space-y-3 px-2">
              {/* People Section */}
              {results.people.length > 0 && (
                <div>
                  <div className="px-2 py-1 text-[10px] font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                    <Users size={12} className="text-brand-500" />
                    <span>People ({results.people.length})</span>
                  </div>
                  <div className="space-y-0.5 mt-0.5">
                    {results.people.map((item) => (
                      <button
                        key={item.id}
                        onClick={() => handleSelectResult(item)}
                        className="w-full text-left px-2.5 py-1.5 rounded-lg hover:bg-slate-50 flex items-center justify-between group transition-colors"
                      >
                        <div className="min-w-0 flex-1 pr-2">
                          <p className="text-xs font-medium text-slate-800 group-hover:text-brand-600 truncate">
                            {item.title}
                          </p>
                          {item.subtitle && (
                            <p className="text-[11px] text-slate-400 truncate">{item.subtitle}</p>
                          )}
                        </div>
                        {item.badge && (
                          <span className="text-[10px] font-mono text-slate-400 bg-slate-100 px-1.5 py-0.5 rounded shrink-0">
                            {item.badge}
                          </span>
                        )}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Channels Section */}
              {results.channels.length > 0 && (
                <div>
                  <div className="px-2 py-1 text-[10px] font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                    <Hash size={12} className="text-indigo-500" />
                    <span>Channels ({results.channels.length})</span>
                  </div>
                  <div className="space-y-0.5 mt-0.5">
                    {results.channels.map((item) => (
                      <button
                        key={item.id}
                        onClick={() => handleSelectResult(item)}
                        className="w-full text-left px-2.5 py-1.5 rounded-lg hover:bg-slate-50 flex items-center justify-between group transition-colors"
                      >
                        <div className="min-w-0 flex-1 pr-2">
                          <p className="text-xs font-medium text-slate-800 group-hover:text-brand-600 truncate">
                            #{item.title}
                          </p>
                          {item.subtitle && (
                            <p className="text-[11px] text-slate-400 truncate">{item.subtitle}</p>
                          )}
                        </div>
                        <ArrowRight size={13} className="text-slate-300 group-hover:text-brand-600 shrink-0" />
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Announcements Section */}
              {results.announcements.length > 0 && (
                <div>
                  <div className="px-2 py-1 text-[10px] font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                    <Megaphone size={12} className="text-amber-500" />
                    <span>Announcements ({results.announcements.length})</span>
                  </div>
                  <div className="space-y-0.5 mt-0.5">
                    {results.announcements.map((item) => (
                      <button
                        key={item.id}
                        onClick={() => handleSelectResult(item)}
                        className="w-full text-left px-2.5 py-1.5 rounded-lg hover:bg-slate-50 flex items-center justify-between group transition-colors"
                      >
                        <div className="min-w-0 flex-1 pr-2">
                          <p className="text-xs font-medium text-slate-800 group-hover:text-brand-600 truncate">
                            {item.title}
                          </p>
                          {item.subtitle && (
                            <p className="text-[11px] text-slate-400 truncate">{item.subtitle}</p>
                          )}
                        </div>
                        {item.badge && (
                          <span className="text-[10px] font-semibold text-amber-700 bg-amber-50 border border-amber-200 px-1.5 py-0.5 rounded shrink-0">
                            {item.badge}
                          </span>
                        )}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Messages Section */}
              {results.messages.length > 0 && (
                <div>
                  <div className="px-2 py-1 text-[10px] font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                    <MessageSquare size={12} className="text-blue-500" />
                    <span>Messages ({results.messages.length})</span>
                  </div>
                  <div className="space-y-0.5 mt-0.5">
                    {results.messages.map((item) => (
                      <button
                        key={item.id}
                        onClick={() => handleSelectResult(item)}
                        className="w-full text-left px-2.5 py-1.5 rounded-lg hover:bg-slate-50 flex items-center justify-between group transition-colors"
                      >
                        <div className="min-w-0 flex-1 pr-2">
                          <p className="text-xs font-medium text-slate-800 group-hover:text-brand-600 truncate">
                            {item.title}
                          </p>
                          {item.subtitle && (
                            <p className="text-[11px] text-slate-400 truncate">{item.subtitle}</p>
                          )}
                        </div>
                        <ArrowRight size={13} className="text-slate-300 group-hover:text-brand-600 shrink-0" />
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
