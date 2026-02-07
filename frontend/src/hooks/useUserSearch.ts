import { useState, useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';
import { paymentService, UserSearchResult } from '../api';
import { useDebouncedValue } from './useDebouncedValue';

interface UseUserSearchOptions {
  excludeUserId: string;
  debounceMs?: number;
  minChars?: number;
}

export const userSearchKeys = {
  all: ['userSearch'] as const,
  search: (query: string, excludeUserId: string) =>
    [...userSearchKeys.all, query, excludeUserId] as const,
};

/**
 * Debounced user search powered by TanStack Query.
 *
 * - Debounces the input so the API is only called after the user stops typing.
 * - Caches results per search term so re-typing the same query is instant.
 * - Loading / error states come from useQuery - no manual try/catch.
 */
export function useUserSearch({
  excludeUserId,
  debounceMs = 300,
  minChars = 2,
}: UseUserSearchOptions) {
  const [query, setQuery] = useState('');
  const debouncedQuery = useDebouncedValue(query, debounceMs);

  const trimmed = debouncedQuery.trim();
  const enabled = trimmed.length >= minChars;

  const { data, isLoading, isFetching, error } = useQuery({
    queryKey: userSearchKeys.search(trimmed, excludeUserId),
    queryFn: () => paymentService.searchUsers(trimmed, excludeUserId),
    enabled,
    staleTime: 60 * 1000, // Cache search results for 60s
    placeholderData: (prev) => prev, // Keep previous results while fetching new ones
  });

  const results: UserSearchResult[] = enabled ? (data?.content ?? []) : [];

  const search = useCallback((value: string) => {
    setQuery(value);
  }, []);

  const clear = useCallback(() => {
    setQuery('');
  }, []);

  return {
    query,
    debouncedQuery: trimmed,
    results,
    isLoading: enabled && (isLoading || isFetching),
    error: error ? 'Failed to search users' : null,
    search,
    clear,
  };
}
