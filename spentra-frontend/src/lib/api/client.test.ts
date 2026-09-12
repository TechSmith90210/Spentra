import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { getLoginErrorMessage, login } from './auth';
import { ApiError, apiClient } from './client';
import { SPENTRA_TOKEN_KEY, SPENTRA_USER_KEY } from '@/lib/constants/auth';

const storage = new Map<string, string>();
const location = { href: '/dashboard' };

function mockErrorResponse(status = 401, message = 'Server error'): Response {
  return {
    ok: false,
    status,
    statusText: 'Unauthorized',
    json: vi.fn().mockResolvedValue({
      message,
      statusCode: status,
      timestamp: '2026-09-13T00:00:00.000Z',
    }),
  } as unknown as Response;
}

beforeEach(() => {
  storage.clear();
  location.href = '/dashboard';
  vi.stubGlobal('window', { location });
  vi.stubGlobal('localStorage', {
    getItem: (key: string) => storage.get(key) ?? null,
    setItem: (key: string, value: string) => storage.set(key, value),
    removeItem: (key: string) => storage.delete(key),
  });
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe('apiClient 401 handling', () => {
  it('clears the session and redirects after an authenticated request receives 401', async () => {
    storage.set(SPENTRA_TOKEN_KEY, 'expired-token');
    storage.set(SPENTRA_USER_KEY, 'legacy-user');
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(mockErrorResponse()));

    await expect(apiClient('/api/expenses')).rejects.toMatchObject({
      name: 'ApiError',
      statusCode: 401,
    });

    expect(storage.has(SPENTRA_TOKEN_KEY)).toBe(false);
    expect(storage.has(SPENTRA_USER_KEY)).toBe(false);
    expect(location.href).toBe('/login');
  });

  it('keeps credentials and location intact for an invalid login 401', async () => {
    storage.set(SPENTRA_TOKEN_KEY, 'stale-token');
    storage.set(SPENTRA_USER_KEY, 'legacy-user');
    const fetchMock = vi.fn().mockResolvedValue(mockErrorResponse());
    vi.stubGlobal('fetch', fetchMock);

    await expect(login({ email: 'person@example.com', password: 'wrong' })).rejects.toMatchObject({
      name: 'ApiError',
      statusCode: 401,
    });

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringMatching(/\/api\/auth\/login$/),
      expect.objectContaining({
        headers: expect.not.objectContaining({ Authorization: expect.any(String) }),
      }),
    );
    expect(storage.get(SPENTRA_TOKEN_KEY)).toBe('stale-token');
    expect(storage.get(SPENTRA_USER_KEY)).toBe('legacy-user');
    expect(location.href).toBe('/dashboard');
  });
});

describe('getLoginErrorMessage', () => {
  it('uses the generic credentials message for a login 401', () => {
    expect(
      getLoginErrorMessage(
        new ApiError('The backend message must not be shown', 401, '2026-09-13T00:00:00.000Z'),
      ),
    ).toBe('Invalid email or password. Please try again.');
  });
});
