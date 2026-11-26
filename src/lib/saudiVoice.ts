export type BalootVoiceCall = 'sun' | 'hokm' | 'pass_r1' | 'hokm_r2' | 'pass_r2';

// Diacritized Saudi Baloot bidding calls ONLY (no other phrases are spoken)
const SAUDI_CALL_PHRASES: Record<BalootVoiceCall, string> = {
  sun: 'صَنْ',
  hokm: 'حُكْم',
  pass_r1: 'بَسْ',
  hokm_r2: 'حُكْم ثَانِي',
  pass_r2: 'وَلَا',
};

const FEMALE_VOICE_RE =
  /female|woman|girl|zariyah|laila|layla|mariam|maryam|hoda|salma|fatima|amina|rana|noura|nour|reem|sana|amira|zeina|amal|lana|iman|farah|sara|nada|dina|yasmin|ar-xa-x-ar[aez]|[-_]f([-_]|$)/i;

const SAUDI_MALE_NAME_RE =
  /\b(hamed|naayf|maged|majed|saud|faisal|khalid|turki|bader|fahad|fahed)\b|حامد|نايف|ماجد|سعود|فيصل|خالد/i;

const GULF_MALE_NAME_RE =
  /\b(hamdan|moaz|ali|abdullah|salman|nasser|rashed|sultan|mansour|waleed)\b|حمدان|معاذ|عبدالله|سلطان/i;

const ARABIC_MALE_NAME_RE =
  /\b(tarik|tariq|bassel|shakir|jamal|omar|salim|taim|rami|ayman|youssef|karim|samir|male|man)\b|طارق|باسل|شاكر|جمال|عمر|سالم|ذكر|رجل|ar-xa-x-ar[dcm]/i;

const GULF_LANG_RE = /^ar[-_](SA|AE|KW|QA|BH|OM)\b/i;
const SAUDI_LANG_RE = /^ar[-_]SA\b/i;
const ARABIC_LANG_RE = /^ar(\b|[-_])/i;

interface ScoredVoice {
  voice: SpeechSynthesisVoice;
  score: number;
  isConfirmedMale: boolean;
}

class SaudiVoiceSpeaker {
  private muted = false;
  private audioCtx: AudioContext | null = null;
  private cachedVoices: SpeechSynthesisVoice[] = [];
  private listenersBound = false;

  constructor() {
    this.initVoices();
  }

  private initVoices() {
    if (typeof window === 'undefined' || !('speechSynthesis' in window)) return;
    const synth = window.speechSynthesis;
    const load = () => {
      const list = synth.getVoices();
      if (list && list.length > 0) {
        this.cachedVoices = list;
      }
    };
    load();
    if (!this.listenersBound) {
      this.listenersBound = true;
      if (typeof synth.addEventListener === 'function') {
        synth.addEventListener('voiceschanged', load);
      } else {
        synth.onvoiceschanged = load;
      }
    }
  }

  private pickSaudiMaleVoice(synth: SpeechSynthesis): ScoredVoice | null {
    const live = synth.getVoices();
    const voices = live.length > 0 ? live : this.cachedVoices;
    if (!voices.length) return null;

    const arabicVoices = voices.filter(
      (v) => ARABIC_LANG_RE.test(v.lang) || /arabic|عربي|saudi|سعود/i.test(v.name),
    );
    if (!arabicVoices.length) return null;

    let best: ScoredVoice | null = null;

    for (const voice of arabicVoices) {
      const name = voice.name || '';
      const lang = voice.lang || '';
      const isFemale = FEMALE_VOICE_RE.test(name);
      const isSaudiMale = SAUDI_MALE_NAME_RE.test(name);
      const isGulfMale = GULF_MALE_NAME_RE.test(name);
      const isOtherMale = ARABIC_MALE_NAME_RE.test(name);
      const isConfirmedMale = !isFemale && (isSaudiMale || isGulfMale || isOtherMale);

      let score = 0;

      if (isFemale) {
        // Strictly penalize female voices so ANY male or neutral Arabic voice always wins
        score -= 500;
      } else {
        score += 100;
      }

      if (isSaudiMale) score += 400;
      else if (isGulfMale) score += 320;
      else if (isOtherMale) score += 240;

      if (SAUDI_LANG_RE.test(lang)) score += 120;
      else if (GULF_LANG_RE.test(lang)) score += 90;
      else if (/^ar[-_]001$/i.test(lang)) score += 60; // Apple Maged uses ar-001
      else if (ARABIC_LANG_RE.test(lang)) score += 30;

      // Prefer Natural / Enhanced / Premium voices when available
      if (/natural|enhanced|premium|neural/i.test(name)) score += 40;

      if (!best || score > best.score) {
        best = { voice, score, isConfirmedMale };
      }
    }

    return best;
  }

  setMuted(muted: boolean) {
    this.muted = muted;
    if (muted && typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
  }

  isMuted(): boolean {
    return this.muted;
  }

  /**
   * Speaks ONLY one of the 5 allowed Saudi Baloot bidding calls:
   * صن | حكم | بس | حكم ثاني | ولا
   */
  speakCall(call: BalootVoiceCall) {
    if (this.muted || typeof window === 'undefined') return;
    const text = SAUDI_CALL_PHRASES[call];
    if (!text) return;

    try {
      if ('speechSynthesis' in window) {
        const synth = window.speechSynthesis;
        this.initVoices();
        synth.cancel();

        const utter = new SpeechSynthesisUtterance(text);
        const picked = this.pickSaudiMaleVoice(synth);

        if (picked) {
          utter.voice = picked.voice;
          // Match the selected voice's exact lang so Safari/Chrome don't override it with a default female voice
          utter.lang = picked.voice.lang || 'ar-SA';
          if (picked.isConfirmedMale) {
            // Natural, crisp Saudi male Baloot call cadence
            utter.pitch = 0.86;
            utter.rate = 0.96;
          } else {
            // Device only exposes neutral/female Arabic voice — deepen pitch strongly toward male timbre
            utter.pitch = 0.52;
            utter.rate = 0.94;
          }
        } else {
          utter.lang = 'ar-SA';
          utter.pitch = 0.65;
          utter.rate = 0.95;
        }

        utter.volume = 1.0;
        synth.speak(utter);
      }
    } catch {
      // Speech synthesis may be unavailable in restricted browsers
    }
  }

  playCardSnap() {
    if (this.muted || typeof window === 'undefined') return;
    try {
      const Ctx =
        window.AudioContext ||
        (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      if (!Ctx) return;
      if (!this.audioCtx) this.audioCtx = new Ctx();
      if (this.audioCtx.state === 'suspended') {
        void this.audioCtx.resume();
      }
      const now = this.audioCtx.currentTime;
      const osc = this.audioCtx.createOscillator();
      const gain = this.audioCtx.createGain();
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(220, now);
      osc.frequency.exponentialRampToValueAtTime(90, now + 0.065);
      gain.gain.setValueAtTime(0.12, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.07);
      osc.connect(gain);
      gain.connect(this.audioCtx.destination);
      osc.start(now);
      osc.stop(now + 0.075);
    } catch {
      // Ignore audio autoplay constraints
    }
  }

  playTurnTick() {
    if (this.muted || typeof window === 'undefined') return;
    try {
      const Ctx =
        window.AudioContext ||
        (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      if (!Ctx) return;
      if (!this.audioCtx) this.audioCtx = new Ctx();
      const now = this.audioCtx.currentTime;
      const osc = this.audioCtx.createOscillator();
      const gain = this.audioCtx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(640, now);
      gain.gain.setValueAtTime(0.05, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.06);
      osc.connect(gain);
      gain.connect(this.audioCtx.destination);
      osc.start(now);
      osc.stop(now + 0.065);
    } catch {
      // Ignore audio constraints
    }
  }
}

export const saudiVoice = new SaudiVoiceSpeaker();
