import React, { useEffect, useMemo, useState } from 'react';
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  limit,
  onSnapshot,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where
} from 'firebase/firestore';
import type { User } from 'firebase/auth';
import { QRCodeSVG } from 'qrcode.react';
import {
  BarChart3,
  Copy,
  DoorOpen,
  ExternalLink,
  Hand,
  Headphones,
  Lock,
  LogIn,
  MessageSquare,
  Mic,
  Pause,
  Pin,
  Play,
  Plus,
  QrCode,
  Send,
  Shield,
  Sparkles,
  Trash2,
  Unlock,
  Users,
  Video
} from 'lucide-react';
import {
  buildJitsiVoiceUrl,
  db,
  detectBrowserPlatform,
  normalizeJoinCode,
  signInWithGoogleBrowser
} from '../firebase';
import type {
  AuraLocalVault,
  CloudParticipant,
  CloudRoomMessage,
  CloudSharedCard,
  CloudStudyRoom
} from '../types';
import { uidGen } from '../storage';

interface Props {
  user: User | null;
  vault: AuraLocalVault;
  initialRoomCode: string;
  onUpdateVault: (updater: (prev: AuraLocalVault) => AuraLocalVault) => void;
  onNotify: (msg: string) => void;
}

const JOIN_CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

function generateJoinCode(): string {
  let out = '';
  for (let i = 0; i < 6; i++) {
    out += JOIN_CHARS[Math.floor(Math.random() * JOIN_CHARS.length)];
  }
  return out;
}

function mapDocToRoom(id: string, d: Record<string, unknown>): CloudStudyRoom {
  return {
    id,
    ownerId: String(d.ownerId || ''),
    ownerName: String(d.ownerName || 'Host'),
    courseCode: String(d.courseCode || 'GEN'),
    roomTitle: String(d.roomTitle || 'Study Stage'),
    focusTopic: String(d.focusTopic || ''),
    status: String(d.status || 'LIVE'),
    visibility: String(d.visibility || 'CAMPUS'),
    activeMinutes: Number(d.activeMinutes || 45),
    joinCode: String(d.joinCode || id.replace(/^room_/, '')),
    timerState: String(d.timerState || 'IDLE'),
    timerEndEpochMillis: Number(d.timerEndEpochMillis || 0),
    timerRemainingSeconds: Number(d.timerRemainingSeconds || 2700),
    sharedBoardNote: String(d.sharedBoardNote || ''),
    waitingRoomEnabled: Boolean(d.waitingRoomEnabled),
    whiteboardLocked: Boolean(d.whiteboardLocked),
    coHostIds: Array.isArray(d.coHostIds) ? (d.coHostIds as string[]) : [],
    voiceRoomUrl: String(d.voiceRoomUrl || ''),
    activePollQuestion: String(d.activePollQuestion || ''),
    activePollOptions: Array.isArray(d.activePollOptions) ? (d.activePollOptions as string[]) : [],
    activePollCorrectIndex:
      typeof d.activePollCorrectIndex === 'number' ? d.activePollCorrectIndex : -1,
    activePollRevealed: Boolean(d.activePollRevealed),
    spotlightCardText: String(d.spotlightCardText || '')
  };
}

export const LiveStudyStage: React.FC<Props> = ({
  user,
  vault,
  initialRoomCode,
  onUpdateVault,
  onNotify
}) => {
  const [campusRooms, setCampusRooms] = useState<CloudStudyRoom[]>([]);
  const [selectedRoomId, setSelectedRoomId] = useState<string | null>(null);
  const [liveRoom, setLiveRoom] = useState<CloudStudyRoom | null>(null);
  const [participants, setParticipants] = useState<CloudParticipant[]>([]);
  const [messages, setMessages] = useState<CloudRoomMessage[]>([]);
  const [sharedCards, setSharedCards] = useState<CloudSharedCard[]>([]);

  const [joinCodeInput, setJoinCodeInput] = useState(initialRoomCode);
  const [cityBadge, setCityBadge] = useState(vault.profile.institution || 'Remote Browser');
  const [autoJoinedInitial, setAutoJoinedInitial] = useState(false);

  // Stage UI state
  const [stageTab, setStageTab] = useState<'BOARD' | 'CHAT' | 'CARDS'>('BOARD');
  const [boardDraft, setBoardDraft] = useState('');
  const [chatText, setChatText] = useState('');
  const [showEmbedVoice, setShowEmbedVoice] = useState(false);
  const [showQrModal, setShowQrModal] = useState(false);
  const [showHostModal, setShowHostModal] = useState(false);
  const [showPollModal, setShowPollModal] = useState(false);
  const [showSpotlightModal, setShowSpotlightModal] = useState(false);
  const [showCardModal, setShowCardModal] = useState(false);
  const [liveSeconds, setLiveSeconds] = useState(0);

  // Host creation form
  const [hCourse, setHCourse] = useState(vault.courses[0]?.code || 'CS301');
  const [hTitle, setHTitle] = useState('');
  const [hTopic, setHTopic] = useState('');
  const [hMins, setHMins] = useState('50');
  const [hVis, setHVis] = useState<'CAMPUS' | 'UNLISTED'>('CAMPUS');
  const [hWaitRoom, setHWaitRoom] = useState(false);
  const [hLockBoard, setHLockBoard] = useState(false);

  // Poll form
  const [pollQ, setPollQ] = useState('');
  const [pollA, setPollA] = useState('');
  const [pollB, setPollB] = useState('');
  const [pollC, setPollC] = useState('');
  const [pollD, setPollD] = useState('');
  const [pollCorrect, setPollCorrect] = useState<number>(0);

  // Spotlight & Card forms
  const [spotlightDraft, setSpotlightDraft] = useState('');
  const [cardQ, setCardQ] = useState('');
  const [cardA, setCardA] = useState('');

  const platformBadge = useMemo(() => detectBrowserPlatform(), []);

  // Observe Open Campus Lobby when signed in
  useEffect(() => {
    if (!user) {
      setCampusRooms([]);
      return;
    }
    const q = query(collection(db, 'study_rooms'), where('visibility', '==', 'CAMPUS'), limit(30));
    const unsub = onSnapshot(
      q,
      (snap) => {
        setCampusRooms(snap.docs.map((d) => mapDocToRoom(d.id, d.data())));
      },
      () => {
        // Ignore transient permission errors
      }
    );
    return () => unsub();
  }, [user]);

  // Observe Active Room Document
  useEffect(() => {
    if (!user || !selectedRoomId) {
      setLiveRoom(null);
      return;
    }
    const ref = doc(db, 'study_rooms', selectedRoomId);
    const unsub = onSnapshot(
      ref,
      (snap) => {
        if (!snap.exists()) {
          setLiveRoom(null);
          return;
        }
        const mapped = mapDocToRoom(snap.id, snap.data());
        setLiveRoom(mapped);
        setBoardDraft(mapped.sharedBoardNote);
      },
      () => setLiveRoom(null)
    );
    return () => unsub();
  }, [user, selectedRoomId]);

  // Observe Subcollections (participants, messages, shared_cards) with rule-compliant filters
  useEffect(() => {
    if (!user || !liveRoom) {
      setParticipants([]);
      setMessages([]);
      setSharedCards([]);
      return;
    }

    const filterField =
      liveRoom.visibility === 'CAMPUS' || liveRoom.visibility === 'UNLISTED'
        ? where('roomVisibility', '==', liveRoom.visibility)
        : where('roomOwnerId', '==', liveRoom.ownerId);

    const pUnsub = onSnapshot(
      query(collection(db, 'study_rooms', liveRoom.id, 'participants'), filterField, limit(60)),
      (snap) => {
        setParticipants(
          snap.docs.map((d) => {
            const x = d.data();
            return {
              id: d.id,
              userId: String(x.userId || d.id),
              displayName: String(x.displayName || 'Scholar'),
              cityOrCampus: String(x.cityOrCampus || 'Remote'),
              studyStatus: String(x.studyStatus || 'FOCUSING'),
              handRaised: Boolean(x.handRaised),
              reactionEmoji: String(x.reactionEmoji || '🎯'),
              roomVisibility: String(x.roomVisibility || liveRoom.visibility),
              roomOwnerId: String(x.roomOwnerId || liveRoom.ownerId),
              admissionStatus: String(x.admissionStatus || 'ADMITTED'),
              pollVoteIndex: typeof x.pollVoteIndex === 'number' ? x.pollVoteIndex : -1,
              devicePlatform: String(x.devicePlatform || 'WEB_BROWSER')
            };
          })
        );
      }
    );

    const mUnsub = onSnapshot(
      query(collection(db, 'study_rooms', liveRoom.id, 'messages'), filterField, limit(80)),
      (snap) => {
        setMessages(
          snap.docs.map((d) => {
            const x = d.data();
            return {
              id: d.id,
              authorId: String(x.authorId || ''),
              authorName: String(x.authorName || 'Scholar'),
              text: String(x.text || ''),
              messageType: String(x.messageType || 'CHAT'),
              roomVisibility: String(x.roomVisibility || liveRoom.visibility),
              roomOwnerId: String(x.roomOwnerId || liveRoom.ownerId)
            };
          })
        );
      }
    );

    const cUnsub = onSnapshot(
      query(collection(db, 'study_rooms', liveRoom.id, 'shared_cards'), filterField, limit(80)),
      (snap) => {
        setSharedCards(
          snap.docs.map((d) => {
            const x = d.data();
            return {
              id: d.id,
              authorId: String(x.authorId || ''),
              authorName: String(x.authorName || 'Scholar'),
              courseCode: String(x.courseCode || liveRoom.courseCode),
              question: String(x.question || ''),
              answer: String(x.answer || ''),
              roomVisibility: String(x.roomVisibility || liveRoom.visibility),
              roomOwnerId: String(x.roomOwnerId || liveRoom.ownerId)
            };
          })
        );
      }
    );

    return () => {
      pUnsub();
      mUnsub();
      cUnsub();
    };
  }, [user, liveRoom?.id, liveRoom?.visibility, liveRoom?.ownerId]);

  // Real-time countdown ticker
  useEffect(() => {
    if (!liveRoom) return;
    const calc = () => {
      if (liveRoom.timerState === 'RUNNING' && liveRoom.timerEndEpochMillis > 0) {
        return Math.max(0, Math.floor((liveRoom.timerEndEpochMillis - Date.now()) / 1000));
      }
      return liveRoom.timerRemainingSeconds > 0
        ? liveRoom.timerRemainingSeconds
        : liveRoom.activeMinutes * 60;
    };
    setLiveSeconds(calc());
    if (liveRoom.timerState !== 'RUNNING') return;
    const id = window.setInterval(() => setLiveSeconds(calc()), 1000);
    return () => window.clearInterval(id);
  }, [liveRoom?.timerState, liveRoom?.timerEndEpochMillis, liveRoom?.timerRemainingSeconds, liveRoom?.activeMinutes]);

  const upsertMyPresence = async (
    room: CloudStudyRoom,
    status = 'FOCUSING',
    handRaised = false,
    emoji = '👋'
  ) => {
    if (!user) return;
    const pRef = doc(db, 'study_rooms', room.id, 'participants', user.uid);
    const existing = await getDoc(pRef);
    const displayName = (user.displayName || vault.profile.name || 'Scholar').slice(0, 80);
    const city = (cityBadge || 'Remote Browser').slice(0, 80);

    if (existing.exists()) {
      const curAdmission = String(existing.data()?.admissionStatus || 'ADMITTED');
      if (curAdmission === 'REMOVED') {
        throw new Error('The host removed you from this session.');
      }
      await updateDoc(pRef, {
        displayName,
        cityOrCampus: city,
        studyStatus: status,
        handRaised,
        reactionEmoji: emoji.slice(0, 16),
        devicePlatform: platformBadge,
        updatedAt: serverTimestamp()
      });
    } else {
      const isHostOrCoHost = room.ownerId === user.uid || room.coHostIds.includes(user.uid);
      const initialAdmission =
        room.waitingRoomEnabled && !isHostOrCoHost ? 'WAITING' : 'ADMITTED';
      await setDoc(pRef, {
        userId: user.uid,
        displayName,
        cityOrCampus: city,
        studyStatus: status,
        handRaised,
        reactionEmoji: emoji.slice(0, 16),
        roomVisibility: room.visibility,
        roomOwnerId: room.ownerId,
        admissionStatus: initialAdmission,
        pollVoteIndex: -1,
        devicePlatform: platformBadge,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp()
      });
    }
  };

  const joinByCode = async (rawCode: string) => {
    if (!user) return;
    const clean = normalizeJoinCode(rawCode);
    if (clean.length < 4) {
      onNotify('Enter a valid 6-character Join Code.');
      return;
    }
    try {
      const directRef = doc(db, 'study_rooms', `room_${clean}`);
      const snap = await getDoc(directRef);
      let found: CloudStudyRoom | null = snap.exists() ? mapDocToRoom(snap.id, snap.data()) : null;

      if (!found) {
        const fallback = await getDocs(
          query(
            collection(db, 'study_rooms'),
            where('visibility', '==', 'CAMPUS'),
            where('joinCode', '==', clean),
            limit(1)
          )
        );
        if (!fallback.empty) {
          found = mapDocToRoom(fallback.docs[0].id, fallback.docs[0].data());
        }
      }

      if (!found) {
        onNotify(`No live session found with code "${clean}". Check with your host.`);
        return;
      }

      await upsertMyPresence(found, 'FOCUSING', false, '👋');
      setSelectedRoomId(found.id);
      onNotify(
        found.waitingRoomEnabled && found.ownerId !== user.uid
          ? `Knocked on "${found.roomTitle}"! Waiting for host ${found.ownerName} to admit you.`
          : `Joined "${found.roomTitle}" (${found.courseCode})!`
      );
    } catch (e: unknown) {
      onNotify((e as Error)?.message || 'Unable to join study session.');
    }
  };

  // Auto-join if user clicked an invite link (?room=CODE) once signed in!
  useEffect(() => {
    if (user && initialRoomCode && !autoJoinedInitial && !selectedRoomId) {
      setAutoJoinedInitial(true);
      joinByCode(initialRoomCode);
    }
  }, [user, initialRoomCode, autoJoinedInitial, selectedRoomId]);

  const hostRoomFromWeb = async () => {
    if (!user || !hTitle.trim()) return;
    const code = generateJoinCode();
    const roomId = `room_${code}`;
    const mins = Math.max(5, Math.min(360, Number(hMins) || 50));
    const ownerName = (user.displayName || vault.profile.name || 'Host').slice(0, 80);
    const voiceUrl = buildJitsiVoiceUrl(code);

    try {
      await setDoc(doc(db, 'study_rooms', roomId), {
        ownerId: user.uid,
        ownerName,
        courseCode: (hCourse.trim().toUpperCase() || 'GEN').slice(0, 24),
        roomTitle: hTitle.trim().slice(0, 100),
        focusTopic: (hTopic.trim() || 'Cross-Platform Live Study Session').slice(0, 240),
        status: 'LIVE',
        visibility: hVis,
        activeMinutes: mins,
        joinCode: code,
        timerState: 'IDLE',
        timerEndEpochMillis: 0,
        timerRemainingSeconds: mins * 60,
        sharedBoardNote: `Welcome to ${hTitle.trim()}! Drop formulas, past-paper notes, or sprint goals here.`,
        waitingRoomEnabled: hWaitRoom,
        whiteboardLocked: hLockBoard,
        coHostIds: [],
        voiceRoomUrl: voiceUrl,
        activePollQuestion: '',
        activePollOptions: [],
        activePollCorrectIndex: -1,
        activePollRevealed: false,
        spotlightCardText: '',
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp()
      });

      const stub: CloudStudyRoom = {
        id: roomId,
        ownerId: user.uid,
        ownerName,
        courseCode: hCourse.trim().toUpperCase() || 'GEN',
        roomTitle: hTitle.trim(),
        focusTopic: hTopic.trim(),
        status: 'LIVE',
        visibility: hVis,
        activeMinutes: mins,
        joinCode: code,
        timerState: 'IDLE',
        timerEndEpochMillis: 0,
        timerRemainingSeconds: mins * 60,
        sharedBoardNote: '',
        waitingRoomEnabled: hWaitRoom,
        whiteboardLocked: hLockBoard,
        coHostIds: [],
        voiceRoomUrl: voiceUrl,
        activePollQuestion: '',
        activePollOptions: [],
        activePollCorrectIndex: -1,
        activePollRevealed: false,
        spotlightCardText: ''
      };
      await upsertMyPresence(stub, 'FOCUSING', false, '🎙️');
      setSelectedRoomId(roomId);
      setShowHostModal(false);
      onNotify(`Hosted Live Room! Share Code ${code} or QR Link with Android & iPhone classmates.`);
    } catch (e: unknown) {
      onNotify((e as Error)?.message || 'Failed to create room.');
    }
  };

  const leaveStage = async () => {
    if (user && selectedRoomId) {
      try {
        const pRef = doc(db, 'study_rooms', selectedRoomId, 'participants', user.uid);
        const snap = await getDoc(pRef);
        if (snap.exists() && snap.data()?.admissionStatus !== 'REMOVED') {
          await deleteDoc(pRef);
        }
      } catch {
        // ignore
      }
    }
    setSelectedRoomId(null);
    setLiveRoom(null);
    setShowEmbedVoice(false);
  };

  if (!user) {
    return (
      <div className="aura-glass rounded-3xl p-6 text-center space-y-4 border border-emerald-400/35">
        <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-emerald-400 to-sky-500 mx-auto flex items-center justify-center text-slate-950">
          <Video className="w-7 h-7" />
        </div>
        {initialRoomCode && (
          <div className="inline-block px-3 py-1 rounded-full bg-emerald-500/20 border border-emerald-400 text-emerald-300 text-xs font-extrabold">
            INVITE DETECTED • ROOM CODE: {initialRoomCode}
          </div>
        )}
        <h2 className="text-xl font-extrabold">
          Join Live Cross-Platform Study Stage (iPhone, Safari, Chrome & Android)
        </h2>
        <p className="text-xs text-slate-400 max-w-md mx-auto leading-relaxed">
          Sign in with Google to enter the live room, sync timers, raise your hand, vote in pop-quiz polls, co-edit the chalkboard, and talk or share slides—zero APK required!
        </p>
        <button
          onClick={async () => {
            try {
              await signInWithGoogleBrowser();
            } catch (e: unknown) {
              onNotify((e as Error)?.message || 'Google Sign-In failed. Check Authorized Domains in Firebase Auth.');
            }
          }}
          className="px-6 py-3 rounded-2xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-extrabold text-sm inline-flex items-center gap-2"
        >
          <LogIn className="w-4 h-4" /> Sign in with Google {initialRoomCode ? `& Join ${initialRoomCode}` : ''}
        </button>
      </div>
    );
  }

  // Render Active Room Stage
  if (liveRoom) {
    const isHost = liveRoom.ownerId === user.uid;
    const isCoHost = liveRoom.coHostIds.includes(user.uid);
    const canControl = isHost || isCoHost;
    const isLocked = liveRoom.status === 'COMPLETED';
    const myP = participants.find((p) => p.userId === user.uid);
    const myAdmission = canControl
      ? 'ADMITTED'
      : myP?.admissionStatus || (liveRoom.waitingRoomEnabled ? 'WAITING' : 'ADMITTED');

    if (myAdmission === 'REMOVED') {
      return (
        <div className="aura-glass rounded-3xl p-6 text-center space-y-3 border border-rose-500/40">
          <h3 className="font-extrabold text-lg text-rose-400">Session Access Ended</h3>
          <p className="text-xs text-slate-400">The host removed your tile from this session.</p>
          <button
            onClick={leaveStage}
            className="px-5 py-2.5 rounded-xl bg-white/10 text-xs font-bold"
          >
            Back to Lobby
          </button>
        </div>
      );
    }

    if (myAdmission === 'WAITING') {
      return (
        <div className="aura-glass rounded-3xl p-6 text-center space-y-4 border border-amber-400/50">
          <span className="px-3 py-1 rounded-full bg-amber-400/20 text-amber-300 text-xs font-extrabold">
            🚪 WAITING ROOM • KNOCK SENT
          </span>
          <h3 className="text-lg font-extrabold">{liveRoom.roomTitle}</h3>
          <p className="text-xs text-slate-300 max-w-md mx-auto">
            Host <strong>{liveRoom.ownerName}</strong> has enabled the Waiting Room. This screen will open automatically the moment you are admitted!
          </p>
          <button
            onClick={leaveStage}
            className="px-5 py-2.5 rounded-xl bg-white/10 text-xs font-bold"
          >
            Leave Waiting Room
          </button>
        </div>
      );
    }

    const waitingGuests = participants.filter((p) => p.admissionStatus === 'WAITING');
    const admitted = participants.filter(
      (p) => p.admissionStatus !== 'WAITING' && p.admissionStatus !== 'REMOVED'
    );
    const displayCode = liveRoom.joinCode || liveRoom.id.replace(/^room_/, '');
    const shareUrl = `${window.location.origin}/?room=${displayCode}`;
    const voiceUrl = liveRoom.voiceRoomUrl || buildJitsiVoiceUrl(displayCode);

    const hrs = Math.floor(liveSeconds / 3600);
    const mins = Math.floor((liveSeconds % 3600) / 60);
    const secs = liveSeconds % 60;
    const clockText =
      hrs > 0
        ? `${String(hrs).padStart(2, '0')}:${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
        : `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;

    const toggleTimer = async () => {
      if (!canControl || isLocked) return;
      if (liveRoom.timerState === 'RUNNING') {
        const rem = Math.max(0, Math.floor((liveRoom.timerEndEpochMillis - Date.now()) / 1000));
        await updateDoc(doc(db, 'study_rooms', liveRoom.id), {
          timerState: 'PAUSED',
          timerEndEpochMillis: 0,
          timerRemainingSeconds: rem,
          updatedAt: serverTimestamp()
        });
      } else {
        const base =
          liveRoom.timerRemainingSeconds > 0
            ? liveRoom.timerRemainingSeconds
            : liveRoom.activeMinutes * 60;
        await updateDoc(doc(db, 'study_rooms', liveRoom.id), {
          timerState: 'RUNNING',
          timerEndEpochMillis: Date.now() + base * 1000,
          timerRemainingSeconds: base,
          updatedAt: serverTimestamp()
        });
      }
    };

    const moderateGuest = async (uid: string, status: 'ADMITTED' | 'REMOVED') => {
      await updateDoc(doc(db, 'study_rooms', liveRoom.id, 'participants', uid), {
        admissionStatus: status,
        updatedAt: serverTimestamp()
      });
    };

    const votePoll = async (idx: number) => {
      if (isLocked) return;
      await updateDoc(doc(db, 'study_rooms', liveRoom.id, 'participants', user.uid), {
        pollVoteIndex: idx,
        updatedAt: serverTimestamp()
      });
    };

    const totalVotes = admitted.filter(
      (p) => p.pollVoteIndex >= 0 && p.pollVoteIndex < liveRoom.activePollOptions.length
    ).length;

    return (
      <div className="space-y-4">
        <div className="aura-glass rounded-3xl p-4 sm:p-5 space-y-4 border border-emerald-400/40">
          {/* Header */}
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="px-2.5 py-1 rounded-lg bg-sky-500/25 text-sky-300 text-xs font-extrabold">
                {liveRoom.courseCode}
              </span>
              <span className="px-2.5 py-1 rounded-lg bg-emerald-500/20 text-emerald-300 font-mono text-xs font-extrabold">
                CODE: {displayCode}
              </span>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setShowQrModal(true)}
                className="px-3 py-1.5 rounded-xl bg-emerald-500 text-slate-950 text-xs font-extrabold inline-flex items-center gap-1.5"
              >
                <QrCode className="w-3.5 h-3.5" /> Invite / QR
              </button>
              <button
                onClick={leaveStage}
                className="px-3 py-1.5 rounded-xl bg-rose-500/20 text-rose-300 text-xs font-bold inline-flex items-center gap-1"
              >
                <DoorOpen className="w-3.5 h-3.5" /> Leave
              </button>
            </div>
          </div>

          <div>
            <h2 className="text-lg font-extrabold">{liveRoom.roomTitle}</h2>
            <p className="text-xs text-slate-400">
              {liveRoom.focusTopic} • Host: {liveRoom.ownerName} • {admitted.length || 1} Scholar(s) Live
            </p>
          </div>

          {/* Live Voice, Camera & Slide Screen-Share Controls */}
          <div className="flex flex-wrap gap-2">
            <button
              onClick={() => setShowEmbedVoice((v) => !v)}
              className="flex-1 py-2.5 px-3 rounded-2xl bg-sky-500/20 border border-sky-400/50 text-sky-200 text-xs font-extrabold inline-flex items-center justify-center gap-1.5"
            >
              <Mic className="w-4 h-4" />
              {showEmbedVoice ? 'Hide Embedded Voice & Slide Stage' : 'Join Live Voice, Video & Slide Share'}
            </button>
            <a
              href={voiceUrl}
              target="_blank"
              rel="noreferrer"
              className="py-2.5 px-3.5 rounded-2xl bg-white/10 hover:bg-white/15 text-xs font-bold inline-flex items-center gap-1.5"
            >
              <ExternalLink className="w-3.5 h-3.5" /> Full Tab
            </a>
          </div>

          {showEmbedVoice && (
            <div className="rounded-2xl overflow-hidden border border-sky-400/40 bg-black h-[340px]">
              <iframe
                title="AURA Live Voice & Screen Share"
                src={`${voiceUrl}#config.prejoinPageEnabled=false`}
                allow="camera; microphone; fullscreen; display-capture; autoplay"
                className="w-full h-full border-0"
              />
            </div>
          )}

          {/* Waiting Room Host Queue */}
          {canControl && waitingGuests.length > 0 && (
            <div className="p-3.5 rounded-2xl bg-amber-500/15 border border-amber-400 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-extrabold text-amber-200">
                  🚪 Waiting Room ({waitingGuests.length} Knocking)
                </span>
                <button
                  onClick={() => waitingGuests.forEach((g) => moderateGuest(g.userId, 'ADMITTED'))}
                  className="text-xs font-extrabold text-emerald-300 underline"
                >
                  Admit All
                </button>
              </div>
              {waitingGuests.map((g) => (
                <div
                  key={g.userId}
                  className="p-2.5 rounded-xl bg-black/30 flex items-center justify-between gap-2"
                >
                  <div className="text-xs">
                    <strong>{g.displayName}</strong>{' '}
                    <span className="text-slate-400">
                      ({g.cityOrCampus} • {g.devicePlatform === 'IPHONE_WEB' ? '📱 iPhone' : '🌐 Browser'})
                    </span>
                  </div>
                  <div className="flex gap-1.5">
                    <button
                      onClick={() => moderateGuest(g.userId, 'ADMITTED')}
                      className="px-2.5 py-1 rounded-lg bg-emerald-500 text-slate-950 text-[11px] font-extrabold"
                    >
                      Admit
                    </button>
                    <button
                      onClick={() => moderateGuest(g.userId, 'REMOVED')}
                      className="px-2.5 py-1 rounded-lg bg-rose-500/25 text-rose-300 text-[11px] font-bold"
                    >
                      Remove
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Host Controls Bar */}
          {canControl && !isLocked && (
            <div className="flex gap-2 overflow-x-auto no-scrollbar pb-1">
              {isHost && (
                <>
                  <button
                    onClick={() =>
                      updateDoc(doc(db, 'study_rooms', liveRoom.id), {
                        waitingRoomEnabled: !liveRoom.waitingRoomEnabled,
                        updatedAt: serverTimestamp()
                      })
                    }
                    className={`px-3 py-1.5 rounded-full text-[11px] font-bold whitespace-nowrap border ${
                      liveRoom.waitingRoomEnabled
                        ? 'bg-amber-400/20 border-amber-400 text-amber-200'
                        : 'bg-white/5 border-white/15 text-slate-300'
                    }`}
                  >
                    <Shield className="w-3 h-3 inline mr-1" />
                    Knock: {liveRoom.waitingRoomEnabled ? 'ON' : 'OFF'}
                  </button>
                  <button
                    onClick={() =>
                      updateDoc(doc(db, 'study_rooms', liveRoom.id), {
                        whiteboardLocked: !liveRoom.whiteboardLocked,
                        updatedAt: serverTimestamp()
                      })
                    }
                    className={`px-3 py-1.5 rounded-full text-[11px] font-bold whitespace-nowrap border ${
                      liveRoom.whiteboardLocked
                        ? 'bg-violet-500/25 border-violet-400 text-violet-200'
                        : 'bg-white/5 border-white/15 text-slate-300'
                    }`}
                  >
                    {liveRoom.whiteboardLocked ? (
                      <Lock className="w-3 h-3 inline mr-1" />
                    ) : (
                      <Unlock className="w-3 h-3 inline mr-1" />
                    )}
                    {liveRoom.whiteboardLocked ? 'Lecture Board' : 'Open Board'}
                  </button>
                </>
              )}
              <button
                onClick={() => setShowPollModal(true)}
                className="px-3 py-1.5 rounded-full text-[11px] font-bold whitespace-nowrap bg-sky-500/20 border border-sky-400/50 text-sky-200"
              >
                <BarChart3 className="w-3 h-3 inline mr-1" /> Launch Pop-Quiz Poll
              </button>
              <button
                onClick={() => {
                  setSpotlightDraft(liveRoom.spotlightCardText);
                  setShowSpotlightModal(true);
                }}
                className="px-3 py-1.5 rounded-full text-[11px] font-bold whitespace-nowrap bg-emerald-500/20 border border-emerald-400/50 text-emerald-200"
              >
                <Pin className="w-3 h-3 inline mr-1" /> Pin Spotlight
              </button>
            </div>
          )}

          {/* Spotlight Banner */}
          {liveRoom.spotlightCardText && (
            <div className="p-3.5 rounded-2xl bg-amber-500/15 border border-amber-400/60 flex items-start justify-between gap-2">
              <div>
                <div className="text-[10px] font-extrabold uppercase tracking-wider text-amber-300">
                  📌 STAGE SPOTLIGHT QUESTION / FOCUS
                </div>
                <div className="text-xs font-bold mt-1">{liveRoom.spotlightCardText}</div>
              </div>
              {canControl && (
                <button
                  onClick={() =>
                    updateDoc(doc(db, 'study_rooms', liveRoom.id), {
                      spotlightCardText: '',
                      updatedAt: serverTimestamp()
                    })
                  }
                  className="text-xs text-amber-300"
                >
                  ✕
                </button>
              )}
            </div>
          )}

          {/* Live Stage Poll */}
          {liveRoom.activePollQuestion && liveRoom.activePollOptions.length >= 2 && (
            <div className="p-4 rounded-2xl bg-indigo-950/60 border border-violet-400/50 space-y-2.5">
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-extrabold text-violet-300">
                  📊 LIVE POP-QUIZ POLL • {totalVotes} VOTE(S)
                </span>
                {canControl && (
                  <div className="flex gap-2 text-[11px] font-bold">
                    {liveRoom.activePollCorrectIndex >= 0 && (
                      <button
                        onClick={() =>
                          updateDoc(doc(db, 'study_rooms', liveRoom.id), {
                            activePollRevealed: !liveRoom.activePollRevealed,
                            updatedAt: serverTimestamp()
                          })
                        }
                        className="text-emerald-300"
                      >
                        {liveRoom.activePollRevealed ? 'Hide Answer' : 'Reveal Answer'}
                      </button>
                    )}
                    <button
                      onClick={() =>
                        updateDoc(doc(db, 'study_rooms', liveRoom.id), {
                          activePollQuestion: '',
                          activePollOptions: [],
                          activePollCorrectIndex: -1,
                          activePollRevealed: false,
                          updatedAt: serverTimestamp()
                        })
                      }
                      className="text-rose-400"
                    >
                      End
                    </button>
                  </div>
                )}
              </div>
              <div className="font-bold text-sm">{liveRoom.activePollQuestion}</div>
              <div className="space-y-1.5">
                {liveRoom.activePollOptions.map((opt, idx) => {
                  const votes = admitted.filter((p) => p.pollVoteIndex === idx).length;
                  const pct = totalVotes > 0 ? Math.round((votes * 100) / totalVotes) : 0;
                  const myVoted = myP?.pollVoteIndex === idx;
                  const isCorrect =
                    liveRoom.activePollRevealed && liveRoom.activePollCorrectIndex === idx;
                  return (
                    <button
                      key={idx}
                      onClick={() => votePoll(idx)}
                      className={`w-full relative overflow-hidden rounded-xl p-2.5 text-left border text-xs flex items-center justify-between ${
                        isCorrect
                          ? 'border-emerald-400 bg-emerald-500/20'
                          : myVoted
                          ? 'border-sky-400 bg-sky-500/20'
                          : 'border-white/15 bg-white/5'
                      }`}
                    >
                      <span className="font-bold">
                        {['A', 'B', 'C', 'D'][idx]}. {opt} {isCorrect ? '✓' : ''}
                      </span>
                      <span className="font-mono text-[11px] text-sky-300">
                        {pct}% ({votes})
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Synchronized Focus Timer */}
          <div className="p-4 rounded-2xl bg-black/30 border border-emerald-400/35 text-center space-y-2">
            <div className="text-[10px] font-extrabold tracking-widest text-emerald-300 uppercase">
              {liveRoom.timerState === 'RUNNING'
                ? 'LIVE SYNCHRONIZED FOCUS SPRINT'
                : 'SHARED STAGE COUNTDOWN'}
            </div>
            <div className="text-4xl font-mono font-extrabold">{clockText}</div>
            {canControl && !isLocked && (
              <div className="flex justify-center gap-2 pt-1">
                <button
                  onClick={toggleTimer}
                  className="px-4 py-2 rounded-xl bg-emerald-500 text-slate-950 text-xs font-extrabold inline-flex items-center gap-1.5"
                >
                  {liveRoom.timerState === 'RUNNING' ? (
                    <>
                      <Pause className="w-3.5 h-3.5" /> Pause All
                    </>
                  ) : (
                    <>
                      <Play className="w-3.5 h-3.5" /> Start Live Timer
                    </>
                  )}
                </button>
              </div>
            )}
          </div>

          {/* Connected Scholars Strip */}
          <div className="space-y-2">
            <div className="text-xs font-extrabold text-sky-300 flex items-center gap-1.5">
              <Users className="w-4 h-4" /> Connected Scholars ({admitted.length || 1})
            </div>
            <div className="flex gap-2.5 overflow-x-auto no-scrollbar pb-1">
              {admitted.map((p) => {
                const badge =
                  p.devicePlatform === 'IPHONE_WEB'
                    ? '📱 iPhone'
                    : p.devicePlatform === 'ANDROID'
                    ? '🤖 Android'
                    : '🌐 Browser';
                return (
                  <div
                    key={p.userId}
                    className={`min-w-[138px] p-3 rounded-2xl border text-center space-y-1 ${
                      p.handRaised
                        ? 'bg-amber-400/15 border-amber-400'
                        : p.userId === user.uid
                        ? 'bg-emerald-500/10 border-emerald-400/60'
                        : 'bg-white/5 border-white/10'
                    }`}
                  >
                    <div className="flex items-center justify-between text-xs">
                      <span>{p.handRaised ? '✋' : ''}</span>
                      <span>{p.reactionEmoji || '🎯'}</span>
                    </div>
                    <div className="font-bold text-xs truncate">
                      {p.displayName} {p.userId === user.uid ? '(You)' : ''}
                    </div>
                    <div className="text-[10px] text-sky-300 truncate">
                      {p.cityOrCampus} • {badge}
                    </div>
                    <div className="text-[10px] font-extrabold text-emerald-300">
                      {p.userId === liveRoom.ownerId
                        ? `HOST • ${p.studyStatus}`
                        : liveRoom.coHostIds.includes(p.userId)
                        ? `CO-HOST • ${p.studyStatus}`
                        : p.studyStatus}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Raise Hand & Reactions */}
          {!isLocked && (
            <div className="flex items-center gap-2 overflow-x-auto no-scrollbar">
              <button
                onClick={() =>
                  upsertMyPresence(
                    liveRoom,
                    myP?.studyStatus || 'FOCUSING',
                    !myP?.handRaised,
                    !myP?.handRaised ? '✋' : '🎯'
                  )
                }
                className={`px-3.5 py-1.5 rounded-full text-xs font-bold border whitespace-nowrap inline-flex items-center gap-1 ${
                  myP?.handRaised
                    ? 'bg-amber-400/25 border-amber-400 text-amber-200'
                    : 'bg-white/5 border-white/15'
                }`}
              >
                <Hand className="w-3.5 h-3.5" />
                {myP?.handRaised ? 'Hand Raised' : 'Raise Hand'}
              </button>
              {['🔥', '💡', '👏', '✅', '🎯'].map((emo) => (
                <button
                  key={emo}
                  onClick={() =>
                    upsertMyPresence(
                      liveRoom,
                      myP?.studyStatus || 'FOCUSING',
                      Boolean(myP?.handRaised),
                      emo
                    )
                  }
                  className="px-3 py-1.5 rounded-full bg-white/5 hover:bg-white/15 text-sm"
                >
                  {emo}
                </button>
              ))}
            </div>
          )}

          {/* Collaborative Sub-Tabs */}
          <div className="flex gap-2">
            {(
              [
                ['BOARD', 'Shared Pinboard'],
                ['CHAT', `Live Q&A (${messages.length})`],
                ['CARDS', `Flashcards (${sharedCards.length})`]
              ] as const
            ).map(([t, label]) => (
              <button
                key={t}
                onClick={() => setStageTab(t)}
                className={`flex-1 py-2 rounded-xl text-xs font-bold border ${
                  stageTab === t
                    ? 'bg-sky-500/25 border-sky-400 text-white'
                    : 'bg-white/5 border-white/10 text-slate-400'
                }`}
              >
                {label}
              </button>
            ))}
          </div>

          {stageTab === 'BOARD' && (
            <div className="space-y-2">
              {liveRoom.whiteboardLocked && !canControl && (
                <p className="text-[11px] text-amber-300">
                  🔒 Lecture Chalkboard Mode: Only Host & Co-Hosts can edit right now.
                </p>
              )}
              <textarea
                rows={4}
                disabled={isLocked || (liveRoom.whiteboardLocked && !canControl)}
                value={boardDraft}
                onChange={(e) => setBoardDraft(e.target.value)}
                className="w-full p-3 rounded-2xl bg-white/5 border border-white/15 text-xs"
              />
              {(!liveRoom.whiteboardLocked || canControl) && !isLocked && (
                <button
                  onClick={async () => {
                    await updateDoc(doc(db, 'study_rooms', liveRoom.id), {
                      sharedBoardNote: boardDraft.slice(0, 2000),
                      updatedAt: serverTimestamp()
                    });
                    onNotify('Synced pinboard across all Android & Browser screens!');
                  }}
                  className="w-full py-2.5 rounded-xl bg-emerald-500 text-slate-950 text-xs font-extrabold"
                >
                  Sync Pinboard to Room
                </button>
              )}
            </div>
          )}

          {stageTab === 'CHAT' && (
            <div className="space-y-2.5">
              <div className="max-h-56 overflow-y-auto space-y-2 p-2 rounded-2xl bg-black/25">
                {messages.length === 0 ? (
                  <p className="text-xs text-slate-500 p-2">
                    Ask a question or share an insight with the room!
                  </p>
                ) : (
                  messages.slice(-15).map((m) => (
                    <div key={m.id} className="p-2.5 rounded-xl bg-white/5 text-xs">
                      <div className="text-[10px] font-extrabold text-sky-300">{m.authorName}</div>
                      <div className="mt-0.5">{m.text}</div>
                    </div>
                  ))
                )}
              </div>
              {!isLocked && (
                <div className="flex gap-2">
                  <input
                    value={chatText}
                    onChange={(e) => setChatText(e.target.value)}
                    onKeyDown={async (e) => {
                      if (e.key === 'Enter' && chatText.trim()) {
                        const msgId = uidGen('msg');
                        const txt = chatText.trim().slice(0, 600);
                        setChatText('');
                        await setDoc(doc(db, 'study_rooms', liveRoom.id, 'messages', msgId), {
                          authorId: user.uid,
                          authorName: (user.displayName || vault.profile.name || 'Scholar').slice(0, 80),
                          text: txt,
                          messageType: 'CHAT',
                          roomVisibility: liveRoom.visibility,
                          roomOwnerId: liveRoom.ownerId,
                          createdAt: serverTimestamp()
                        });
                      }
                    }}
                    placeholder="Message Android & iPhone classmates..."
                    className="flex-1 px-3 py-2 rounded-xl bg-white/5 border border-white/15 text-xs"
                  />
                  <button
                    onClick={async () => {
                      if (!chatText.trim()) return;
                      const msgId = uidGen('msg');
                      const txt = chatText.trim().slice(0, 600);
                      setChatText('');
                      await setDoc(doc(db, 'study_rooms', liveRoom.id, 'messages', msgId), {
                        authorId: user.uid,
                        authorName: (user.displayName || vault.profile.name || 'Scholar').slice(0, 80),
                        text: txt,
                        messageType: 'CHAT',
                        roomVisibility: liveRoom.visibility,
                        roomOwnerId: liveRoom.ownerId,
                        createdAt: serverTimestamp()
                      });
                    }}
                    className="p-2.5 rounded-xl bg-emerald-500 text-slate-950"
                  >
                    <Send className="w-4 h-4" />
                  </button>
                </div>
              )}
            </div>
          )}

          {stageTab === 'CARDS' && (
            <div className="space-y-2.5">
              {!isLocked && (
                <button
                  onClick={() => setShowCardModal(true)}
                  className="w-full py-2.5 rounded-xl bg-emerald-500 text-slate-950 text-xs font-extrabold"
                >
                  + Drop Flashcard to Room
                </button>
              )}
              {sharedCards.map((c) => (
                <div key={c.id} className="p-3 rounded-2xl bg-white/5 border border-white/10 space-y-1">
                  <div className="flex items-center justify-between text-[11px]">
                    <span className="text-sky-300 font-bold">
                      {c.courseCode} • by {c.authorName}
                    </span>
                    <button
                      onClick={() => {
                        onUpdateVault((prev) => ({
                          ...prev,
                          flashcards: [
                            ...prev.flashcards,
                            {
                              id: uidGen('f'),
                              courseCode: c.courseCode,
                              deckTitle: `Live Room • ${liveRoom.roomTitle}`,
                              question: c.question,
                              answer: c.answer,
                              easeFactor: 2.5,
                              intervalDays: 1,
                              repetitions: 0,
                              nextReviewEpoch: Date.now()
                            }
                          ]
                        }));
                        onNotify('Saved flashcard to your SM-2 Deck!');
                      }}
                      className="text-emerald-300 font-extrabold"
                    >
                      + Save to Deck
                    </button>
                  </div>
                  <div className="text-xs font-bold">Q: {c.question}</div>
                  <div className="text-xs text-slate-300">A: {c.answer}</div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* QR & Link Invite Modal */}
        {showQrModal && (
          <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4">
            <div className="aura-glass rounded-3xl p-5 w-full max-w-sm text-center space-y-4 border border-emerald-400/50">
              <h3 className="font-extrabold text-base">Invite Classmates (iPhone, Web or Android)</h3>
              <div className="bg-white p-4 rounded-2xl inline-block mx-auto">
                <QRCodeSVG value={shareUrl} size={176} />
              </div>
              <div className="font-mono text-2xl font-extrabold text-emerald-400">{displayCode}</div>
              <div className="text-[11px] text-slate-400 break-all">{shareUrl}</div>
              <div className="flex gap-2">
                <button
                  onClick={() => {
                    navigator.clipboard.writeText(shareUrl);
                    onNotify('Copied Browser & iPhone Invite Link!');
                  }}
                  className="flex-1 py-2.5 rounded-xl bg-emerald-500 text-slate-950 text-xs font-extrabold inline-flex items-center justify-center gap-1"
                >
                  <Copy className="w-3.5 h-3.5" /> Copy Link
                </button>
                <button
                  onClick={() => setShowQrModal(false)}
                  className="flex-1 py-2.5 rounded-xl bg-white/10 text-xs font-bold"
                >
                  Done
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Create Poll Modal */}
        {showPollModal && (
          <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4">
            <div className="aura-glass rounded-3xl p-5 w-full max-w-md space-y-2.5 border border-sky-400/40">
              <h3 className="font-extrabold text-base">Launch Live Pop-Quiz Poll</h3>
              <input
                value={pollQ}
                onChange={(e) => setPollQ(e.target.value)}
                placeholder="Question prompt"
                className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/15 text-xs"
              />
              <input value={pollA} onChange={(e) => setPollA(e.target.value)} placeholder="Option A" className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/15 text-xs" />
              <input value={pollB} onChange={(e) => setPollB(e.target.value)} placeholder="Option B" className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/15 text-xs" />
              <input value={pollC} onChange={(e) => setPollC(e.target.value)} placeholder="Option C (Optional)" className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/15 text-xs" />
              <input value={pollD} onChange={(e) => setPollD(e.target.value)} placeholder="Option D (Optional)" className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/15 text-xs" />
              <div className="flex items-center gap-2 text-xs">
                <span>Correct Answer:</span>
                {['A', 'B', 'C', 'D'].map((l, idx) => (
                  <button
                    key={l}
                    onClick={() => setPollCorrect(idx)}
                    className={`px-2.5 py-1 rounded-lg font-bold ${
                      pollCorrect === idx ? 'bg-emerald-500 text-slate-950' : 'bg-white/10'
                    }`}
                  >
                    {l}
                  </button>
                ))}
              </div>
              <div className="flex gap-2 pt-2">
                <button onClick={() => setShowPollModal(false)} className="flex-1 py-2 rounded-xl bg-white/10 text-xs font-bold">Cancel</button>
                <button
                  onClick={async () => {
                    const opts = [pollA, pollB, pollC, pollD].map((s) => s.trim()).filter(Boolean);
                    if (!pollQ.trim() || opts.length < 2) return;
                    await updateDoc(doc(db, 'study_rooms', liveRoom.id), {
                      activePollQuestion: pollQ.trim().slice(0, 300),
                      activePollOptions: opts.slice(0, 4),
                      activePollCorrectIndex: pollCorrect,
                      activePollRevealed: false,
                      updatedAt: serverTimestamp()
                    });
                    setShowPollModal(false);
                  }}
                  className="flex-1 py-2 rounded-xl bg-emerald-500 text-slate-950 text-xs font-extrabold"
                >
                  Push Live
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Pin Spotlight Modal */}
        {showSpotlightModal && (
          <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4">
            <div className="aura-glass rounded-3xl p-5 w-full max-w-md space-y-3">
              <h3 className="font-extrabold text-base">Pin Top Stage Spotlight</h3>
              <textarea
                rows={3}
                value={spotlightDraft}
                onChange={(e) => setSpotlightDraft(e.target.value)}
                placeholder="Paste past-paper question or theorem to pin at the top of everyone's screen..."
                className="w-full p-3 rounded-xl bg-white/5 border border-white/15 text-xs"
              />
              <div className="flex gap-2">
                <button onClick={() => setShowSpotlightModal(false)} className="flex-1 py-2 rounded-xl bg-white/10 text-xs font-bold">Cancel</button>
                <button
                  onClick={async () => {
                    await updateDoc(doc(db, 'study_rooms', liveRoom.id), {
                      spotlightCardText: spotlightDraft.trim().slice(0, 600),
                      updatedAt: serverTimestamp()
                    });
                    setShowSpotlightModal(false);
                  }}
                  className="flex-1 py-2 rounded-xl bg-emerald-500 text-slate-950 text-xs font-extrabold"
                >
                  Pin to Stage
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Drop Shared Card Modal */}
        {showCardModal && (
          <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4">
            <div className="aura-glass rounded-3xl p-5 w-full max-w-md space-y-3">
              <h3 className="font-extrabold text-base">Share Flashcard to Room</h3>
              <input value={cardQ} onChange={(e) => setCardQ(e.target.value)} placeholder="Question" className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/15 text-xs" />
              <textarea rows={3} value={cardA} onChange={(e) => setCardA(e.target.value)} placeholder="Verified Answer" className="w-full p-3 rounded-xl bg-white/5 border border-white/15 text-xs" />
              <div className="flex gap-2">
                <button onClick={() => setShowCardModal(false)} className="flex-1 py-2 rounded-xl bg-white/10 text-xs font-bold">Cancel</button>
                <button
                  onClick={async () => {
                    if (!cardQ.trim() || !cardA.trim()) return;
                    await setDoc(doc(db, 'study_rooms', liveRoom.id, 'shared_cards', uidGen('card')), {
                      authorId: user.uid,
                      authorName: (user.displayName || vault.profile.name || 'Scholar').slice(0, 80),
                      courseCode: liveRoom.courseCode,
                      question: cardQ.trim().slice(0, 500),
                      answer: cardA.trim().slice(0, 1000),
                      roomVisibility: liveRoom.visibility,
                      roomOwnerId: liveRoom.ownerId,
                      createdAt: serverTimestamp()
                    });
                    setCardQ('');
                    setCardA('');
                    setShowCardModal(false);
                  }}
                  className="flex-1 py-2 rounded-xl bg-emerald-500 text-slate-950 text-xs font-extrabold"
                >
                  Drop Card
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }

  // Lobby View (Join by Code + Host Session + Open Campus Rooms)
  return (
    <div className="space-y-4">
      <div className="aura-glass rounded-3xl p-5 space-y-3.5 border border-violet-400/35">
        <div className="flex items-center justify-between">
          <span className="px-2.5 py-1 rounded-full bg-violet-500/20 text-violet-200 text-[11px] font-extrabold inline-flex items-center gap-1.5">
            <Sparkles className="w-3.5 h-3.5" /> ANDROID • IPHONE • CHROME • SAFARI
          </span>
          <span className="text-[11px] text-emerald-300 font-bold">
            {platformBadge === 'IPHONE_WEB' ? '📱 iPhone Ready' : '🌐 Browser Ready'}
          </span>
        </div>
        <h2 className="text-lg font-extrabold">Host or Join Universal Live Study Stage</h2>
        <p className="text-xs text-slate-400">
          Enter a 6-character code from an Android or iPhone host, or start your own synchronized live study session with voice, slide screen-sharing, polls & pinboard.
        </p>

        <input
          value={cityBadge}
          onChange={(e) => setCityBadge(e.target.value)}
          placeholder="Your Town / Campus Badge (e.g. Accra • Legon)"
          className="w-full px-3.5 py-2.5 rounded-xl bg-white/5 border border-white/15 text-xs"
        />

        <div className="flex gap-2">
          <input
            value={joinCodeInput}
            onChange={(e) => setJoinCodeInput(e.target.value)}
            placeholder="6-Char Code or Link (e.g. AUR7K2)"
            className="flex-1 px-3.5 py-2.5 rounded-xl bg-white/5 border border-white/15 text-xs font-mono uppercase"
          />
          <button
            onClick={() => joinByCode(joinCodeInput)}
            className="px-5 py-2.5 rounded-xl bg-emerald-500 text-slate-950 font-extrabold text-xs"
          >
            Join Stage
          </button>
        </div>

        <button
          onClick={() => setShowHostModal(true)}
          className="w-full py-3 rounded-2xl bg-gradient-to-r from-sky-500 to-violet-500 text-white font-extrabold text-xs inline-flex items-center justify-center gap-2"
        >
          <Plus className="w-4 h-4" /> Host New Live Study Session
        </button>
      </div>

      {/* Open Lobby */}
      <div className="space-y-2.5">
        <h3 className="font-extrabold text-sm px-1">Live Open Study Lobby ({campusRooms.length})</h3>
        {campusRooms.length === 0 ? (
          <div className="aura-glass rounded-3xl p-6 text-center text-xs text-slate-400">
            No open sessions right now. Host one above to get a 6-character code & QR link!
          </div>
        ) : (
          campusRooms.map((r) => (
            <div
              key={r.id}
              className="aura-glass rounded-2xl p-4 flex items-center justify-between gap-3"
            >
              <div className="min-w-0 space-y-1">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="px-2 py-0.5 rounded bg-sky-500/20 text-sky-300 text-[11px] font-bold">
                    {r.courseCode}
                  </span>
                  <span className="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 font-mono text-[11px] font-bold">
                    CODE: {r.joinCode}
                  </span>
                  {r.waitingRoomEnabled && (
                    <span className="px-2 py-0.5 rounded bg-amber-400/20 text-amber-300 text-[10px] font-bold">
                      KNOCK
                    </span>
                  )}
                </div>
                <div className="font-bold text-sm truncate">{r.roomTitle}</div>
                <div className="text-xs text-slate-400 truncate">
                  {r.focusTopic} • Host: {r.ownerName}
                </div>
              </div>
              <button
                onClick={async () => {
                  try {
                    await upsertMyPresence(r, 'FOCUSING', false, '👋');
                    setSelectedRoomId(r.id);
                  } catch (e: unknown) {
                    onNotify((e as Error)?.message || 'Could not enter room.');
                  }
                }}
                className="px-4 py-2 rounded-xl bg-emerald-500 text-slate-950 text-xs font-extrabold shrink-0"
              >
                {r.waitingRoomEnabled && r.ownerId !== user.uid ? 'Knock' : 'Enter'}
              </button>
            </div>
          ))
        )}
      </div>

      {/* Host New Room Modal */}
      {showHostModal && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="aura-glass rounded-3xl p-5 w-full max-w-md space-y-3 border border-violet-400/40">
            <h3 className="font-extrabold text-base">Host Cross-Platform Study Stage</h3>
            <div className="grid grid-cols-2 gap-2">
              <input
                value={hCourse}
                onChange={(e) => setHCourse(e.target.value)}
                placeholder="Course Code (CS301)"
                className="px-3 py-2 rounded-xl bg-white/5 border border-white/15 text-xs"
              />
              <input
                type="number"
                value={hMins}
                onChange={(e) => setHMins(e.target.value)}
                placeholder="Minutes (50)"
                className="px-3 py-2 rounded-xl bg-white/5 border border-white/15 text-xs"
              />
            </div>
            <input
              value={hTitle}
              onChange={(e) => setHTitle(e.target.value)}
              placeholder="Session Title (e.g. Calculus Past Paper Sprint)"
              className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/15 text-xs"
            />
            <input
              value={hTopic}
              onChange={(e) => setHTopic(e.target.value)}
              placeholder="Goal / Topic"
              className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/15 text-xs"
            />
            <div className="grid grid-cols-2 gap-2">
              {(['CAMPUS', 'UNLISTED'] as const).map((m) => (
                <button
                  key={m}
                  onClick={() => setHVis(m)}
                  className={`py-2 rounded-xl text-xs font-bold border ${
                    hVis === m ? 'bg-sky-500/25 border-sky-400 text-white' : 'bg-white/5 border-white/10 text-slate-400'
                  }`}
                >
                  {m === 'CAMPUS' ? 'Open Lobby + Link' : 'Private Link/Code Only'}
                </button>
              ))}
            </div>
            <label className="flex items-center gap-2 text-xs">
              <input
                type="checkbox"
                checked={hWaitRoom}
                onChange={(e) => setHWaitRoom(e.target.checked)}
              />
              Enable Waiting Room ("Knock to Join")
            </label>
            <label className="flex items-center gap-2 text-xs">
              <input
                type="checkbox"
                checked={hLockBoard}
                onChange={(e) => setHLockBoard(e.target.checked)}
              />
              Lecture Mode (Host-only Chalkboard)
            </label>
            <div className="flex gap-2 pt-1">
              <button onClick={() => setShowHostModal(false)} className="flex-1 py-2.5 rounded-xl bg-white/10 text-xs font-bold">Cancel</button>
              <button onClick={hostRoomFromWeb} className="flex-1 py-2.5 rounded-xl bg-emerald-500 text-slate-950 text-xs font-extrabold">Go Live</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
