import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  MAX_RECEIPT_SIZE_BYTES,
  parseReceiptImage,
  RECEIPT_SIZE_ERROR,
} from './ai';

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('parseReceiptImage', () => {
  it('rejects files larger than the 2 MB receipt cap without making a request', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);

    await expect(
      parseReceiptImage({ size: MAX_RECEIPT_SIZE_BYTES + 1 } as File),
    ).rejects.toThrow(RECEIPT_SIZE_ERROR);

    expect(fetchMock).not.toHaveBeenCalled();
  });
});
