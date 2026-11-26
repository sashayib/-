import { readFileSync } from 'node:fs';
import path from 'node:path';
import {
  type App,
  applicationDefault,
  cert,
  getApps,
  initializeApp,
  type ServiceAccount,
} from 'firebase-admin/app';
import { type Auth, type DecodedIdToken, getAuth } from 'firebase-admin/auth';
import { type Firestore, getFirestore } from 'firebase-admin/firestore';

interface AppletFirebaseConfig {
  projectId?: string;
  firestoreDatabaseId?: string;
}

function loadAppletConfig(): AppletFirebaseConfig {
  try {
    const configPath = path.resolve(process.cwd(), 'firebase-applet-config.json');
    const raw = readFileSync(configPath, 'utf-8');
    return JSON.parse(raw) as AppletFirebaseConfig;
  } catch {
    return {};
  }
}

export interface FirebaseAdminContext {
  ready: boolean;
  projectId: string;
  databaseId: string;
  credentialSource: 'service_account_json' | 'adc' | 'unconfigured';
  initError?: string;
  app?: App;
  auth?: Auth;
  db?: Firestore;
}

let cachedContext: FirebaseAdminContext | null = null;

/**
 * Initializes Firebase Admin SDK using server-side credentials only:
 * 1. FIREBASE_SERVICE_ACCOUNT_JSON (JSON string from Secret Manager / env)
 * 2. Application Default Credentials (GOOGLE_APPLICATION_CREDENTIALS or Cloud Run service account)
 * Never uses or logs hardcoded keys.
 */
export function getFirebaseAdmin(): FirebaseAdminContext {
  if (cachedContext) return cachedContext;

  const appletCfg = loadAppletConfig();
  const projectId = (process.env.FIREBASE_PROJECT_ID || appletCfg.projectId || '').trim();
  const databaseId = (process.env.FIRESTORE_DATABASE_ID || appletCfg.firestoreDatabaseId || '(default)').trim();

  try {
    let app: App;
    let credentialSource: FirebaseAdminContext['credentialSource'] = 'adc';

    if (getApps().length > 0) {
      app = getApps()[0];
    } else if (process.env.FIREBASE_SERVICE_ACCOUNT_JSON?.trim()) {
      const parsed = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON) as ServiceAccount;
      app = initializeApp({
        credential: cert(parsed),
        projectId: projectId || parsed.projectId,
      });
      credentialSource = 'service_account_json';
    } else {
      app = initializeApp({
        credential: applicationDefault(),
        projectId: projectId || undefined,
      });
      credentialSource = 'adc';
    }

    const auth = getAuth(app);
    const db =
      databaseId && databaseId !== '(default)'
        ? getFirestore(app, databaseId)
        : getFirestore(app);

    cachedContext = {
      ready: true,
      projectId,
      databaseId,
      credentialSource,
      app,
      auth,
      db,
    };
    return cachedContext;
  } catch (err) {
    cachedContext = {
      ready: false,
      projectId,
      databaseId,
      credentialSource: 'unconfigured',
      initError: err instanceof Error ? err.message : 'Failed to initialize Firebase Admin SDK.',
    };
    return cachedContext;
  }
}

export interface VerifiedUser {
  uid: string;
  email: string | null;
  displayName: string;
  avatarUrl: string | null;
}

/**
 * Verifies a Firebase Authentication ID Token strictly on the server.
 * Always checks token signature, expiration, and project audience via Firebase Admin Auth.
 */
export async function verifyFirebaseIdToken(idToken: string): Promise<VerifiedUser> {
  const ctx = getFirebaseAdmin();
  if (!ctx.ready || !ctx.auth) {
    throw new Error('FIREBASE_ADMIN_UNCONFIGURED');
  }

  const decoded: DecodedIdToken = await ctx.auth.verifyIdToken(idToken, true);
  const fallbackName =
    (typeof decoded.name === 'string' && decoded.name.trim()) ||
    (typeof decoded.email === 'string' && decoded.email.split('@')[0]?.trim()) ||
    'لاعب شدها';

  return {
    uid: decoded.uid,
    email: typeof decoded.email === 'string' ? decoded.email : null,
    displayName: fallbackName.slice(0, 40),
    avatarUrl: typeof decoded.picture === 'string' ? decoded.picture.slice(0, 500) : null,
  };
}

export function requireAdminFirestore(): Firestore {
  const ctx = getFirebaseAdmin();
  if (!ctx.ready || !ctx.db) {
    throw new Error('FIREBASE_ADMIN_UNCONFIGURED');
  }
  return ctx.db;
}
