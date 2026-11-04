import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { onAuthStateChanged, type User } from 'firebase/auth';
import {
  BookOpen,
  Calendar,
  Flame,
  Sliders,
  Video
} from 'lucide-react';
import { auth, normalizeJoinCode } from './firebase';
import {
  fetchAndMergeCloudBackupToWeb,
  loadLocalVault,
  saveLocalVault
} from './storage';
import type { AuraLocalVault } from './types';
import { TimetableView } from './components/TimetableView';
import { FocusView } from './components/FocusView';
import { StudyHubView } from './components/StudyHubView';
import { LiveStudyStage } from './components/LiveStudyStage';
import { ToolsAndProfileView } from './components/ToolsAndProfileView';

type MainTab = 'TIMETABLE' | 'FOCUS' | 'STUDY' | 'LIVE_STAGE' | 'TOOLS';

export const App: React.FC = () => {
  const initialRoomFromUrl = useMemo(() => {
    if (typeof window === 'undefined') return '';
    const params = new URLSearchParams(window.location.search);
    const code = params.get('room') || '';
    return normalizeJoinCode(code);
  }, []);

  const [activeTab, setActiveTab] = useState<MainTab>(
    initialRoomFromUrl ? 'LIVE_STAGE' : 'TIMETABLE'
  );
  const [vault, setVault] = useState<AuraLocalVault>(() => loadLocalVault());
  const [user, setUser] = useState<User | null>(null);
  const [toast, setToast] = useState<string>('');

  const notify = useCallback((msg: string) => {
    setToast(msg);
  }, []);

  useEffect(() => {
    if (!toast) return;
    const t = window.setTimeout(() => setToast(''), 4500);
    return () => window.clearTimeout(t);
  }, [toast]);

  const updateVault = useCallback((updater: (prev: AuraLocalVault) => AuraLocalVault) => {
    setVault((prev) => {
      const next = updater(prev);
      saveLocalVault(next);
      return next;
    });
  }, []);

  // Sync theme class on <html>
  useEffect(() => {
    const html = document.documentElement;
    if (vault.profile.isDarkTheme) {
      html.classList.add('dark');
      html.classList.remove('light');
    } else {
      html.classList.add('light');
      html.classList.remove('dark');
    }
  }, [vault.profile.isDarkTheme]);

  // Listen to Firebase Google Auth & auto-import Cloud Backup once per UID
  useEffect(() => {
    const unsub = onAuthStateChanged(auth, async (u) => {
      setUser(u);
      if (u) {
        const current = loadLocalVault();
        if (current.lastAutoRestoredUid !== u.uid) {
          try {
            const res = await fetchAndMergeCloudBackupToWeb(u.uid, current);
            if (res) {
              setVault(res.updatedVault);
              saveLocalVault(res.updatedVault);
              notify(`Welcome back! Auto-synced your Android Cloud Vault (${res.summary})`);
            }
          } catch {
            // Silent if offline or no backup yet
          }
        }
      }
    });
    return () => unsub();
  }, [notify]);

  const navItems: { id: MainTab; label: string; icon: React.ReactNode }[] = [
    { id: 'TIMETABLE', label: 'Timetable', icon: <Calendar className="w-4 h-4" /> },
    { id: 'FOCUS', label: 'Focus', icon: <Flame className="w-4 h-4" /> },
    { id: 'STUDY', label: 'Study Hub', icon: <BookOpen className="w-4 h-4" /> },
    { id: 'LIVE_STAGE', label: 'Live Meet', icon: <Video className="w-4 h-4" /> },
    { id: 'TOOLS', label: 'Sync & Tools', icon: <Sliders className="w-4 h-4" /> }
  ];

  return (
    <div className="min-h-dvh flex flex-col max-w-4xl mx-auto px-3 sm:px-6 pb-24 pt-3">
      {/* Top App Bar */}
      <header className="aura-glass rounded-2xl px-4 py-3 mb-4 flex items-center justify-between gap-3">
        <div className="flex items-center gap-2.5 min-w-0">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-emerald-400 via-sky-400 to-violet-500 flex items-center justify-center font-extrabold text-slate-950 text-sm shrink-0">
            A
          </div>
          <div className="min-w-0">
            <div className="font-extrabold text-sm tracking-tight truncate">
              AURA • Academic Companion
            </div>
            <div className="text-[11px] text-slate-400 truncate">
              {vault.profile.name} • {vault.profile.institution}
            </div>
          </div>
        </div>

        <button
          onClick={() => setActiveTab('LIVE_STAGE')}
          className="px-3 py-1.5 rounded-xl bg-emerald-500/20 border border-emerald-400/50 text-emerald-300 text-xs font-extrabold shrink-0"
        >
          {user ? '🟢 Cloud Connected' : 'Join / Sign In'}
        </button>
      </header>

      {/* Toast Notification */}
      {toast && (
        <div
          onClick={() => setToast('')}
          className="mb-4 p-3 rounded-2xl bg-emerald-500/20 border border-emerald-400 text-xs font-bold text-emerald-100 cursor-pointer"
        >
          {toast}
        </div>
      )}

      {/* Main Content */}
      <main className="flex-1">
        {activeTab === 'TIMETABLE' && (
          <TimetableView vault={vault} onUpdateVault={updateVault} />
        )}
        {activeTab === 'FOCUS' && (
          <FocusView vault={vault} onUpdateVault={updateVault} />
        )}
        {activeTab === 'STUDY' && (
          <StudyHubView vault={vault} onUpdateVault={updateVault} />
        )}
        {activeTab === 'LIVE_STAGE' && (
          <LiveStudyStage
            user={user}
            vault={vault}
            initialRoomCode={initialRoomFromUrl}
            onUpdateVault={updateVault}
            onNotify={notify}
          />
        )}
        {activeTab === 'TOOLS' && (
          <ToolsAndProfileView
            user={user}
            vault={vault}
            onUpdateVault={updateVault}
            onNotify={notify}
          />
        )}
      </main>

      {/* Bottom Liquid Glass Navigation Bar (iPhone Safe-Area Friendly) */}
      <nav className="fixed bottom-3 left-3 right-3 max-w-4xl mx-auto z-40 aura-glass rounded-2xl p-1.5 grid grid-cols-5 gap-1 shadow-2xl">
        {navItems.map((item) => {
          const active = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => setActiveTab(item.id)}
              className={`py-2 px-1 rounded-xl flex flex-col items-center justify-center gap-1 transition ${
                active
                  ? 'bg-emerald-500 text-slate-950 font-extrabold'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              {item.icon}
              <span className="text-[10px] leading-none">{item.label}</span>
            </button>
          );
        })}
      </nav>
    </div>
  );
};
