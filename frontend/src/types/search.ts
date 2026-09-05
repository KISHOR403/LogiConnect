export type SearchCategory = 'all' | 'people' | 'messages' | 'channels' | 'announcements' | 'documents' | 'meetings';

export interface SearchResultItem {
  id: string;
  title: string;
  subtitle?: string;
  category: SearchCategory;
  url: string;
  metadata?: string;
  badge?: string;
}

export interface GroupedSearchResults {
  people: SearchResultItem[];
  channels: SearchResultItem[];
  announcements: SearchResultItem[];
  messages: SearchResultItem[];
  documents: SearchResultItem[];
  meetings: SearchResultItem[];
}
