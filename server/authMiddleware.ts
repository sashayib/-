import type { NextFunction, Request, Response } from 'express';
import { type VerifiedUser, verifyFirebaseIdToken } from './firebaseAdmin.ts';

export interface AuthenticatedRequest extends Request {
  user?: VerifiedUser;
}

export function extractBearerToken(authHeader: string | undefined): string | null {
  if (!authHeader || typeof authHeader !== 'string') return null;
  const parts = authHeader.trim().split(/\s+/);
  if (parts.length !== 2 || parts[0].toLowerCase() !== 'bearer') return null;
  const token = parts[1].trim();
  if (token.length < 20 || token.length > 8192) return null;
  return token;
}

/**
 * Express middleware that requires a valid Firebase Authentication ID Token
 * in the `Authorization: Bearer <idToken>` header.
 */
export async function requireFirebaseAuth(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction,
): Promise<void> {
  const token = extractBearerToken(req.headers.authorization);
  if (!token) {
    res.status(401).json({
      ok: false,
      code: 'UNAUTHENTICATED',
      message: 'Missing or malformed Authorization Bearer token.',
    });
    return;
  }

  try {
    req.user = await verifyFirebaseIdToken(token);
    next();
  } catch (err) {
    if (err instanceof Error && err.message === 'FIREBASE_ADMIN_UNCONFIGURED') {
      res.status(503).json({
        ok: false,
        code: 'SERVER_NOT_CONFIGURED',
        message: 'Firebase Admin credentials are not configured on this server.',
      });
      return;
    }
    res.status(401).json({
      ok: false,
      code: 'AUTH_FAILED',
      message: 'Invalid or expired Firebase ID Token.',
    });
  }
}
