import React, { useEffect, useMemo, useState } from 'react';
import { api } from '../api/client';

type Props = {
  open: boolean;
  initialNickname?: string;
  onClose: () => void;
  onSaved: (newNickname: string) => void;
};

function debounce<Args extends unknown[]>(fn: (...args: Args) => void, ms = 350) {
  let t: ReturnType<typeof setTimeout> | undefined;
  return (...args: Args) => {
    if (t) clearTimeout(t);
    t = setTimeout(() => fn(...args), ms);
  };
}

const NicknameDialog: React.FC<Props> = ({ open, initialNickname, onClose, onSaved }) => {
  const [nick, setNick] = useState(initialNickname ?? '');
  const [checking, setChecking] = useState(false);
  const [available, setAvailable] = useState<boolean | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setNick(initialNickname ?? '');
    setAvailable(null);
    setError(null);
  }, [open, initialNickname]);

  const validateFormat = (v: string) => {
    if (!v || v.trim().length < 2) return '닉네임은 2자 이상이어야 해요.';
    if (v.length > 30) return '닉네임은 최대 30자까지 가능합니다.';
    if (!/^[\w가-힣\- ]+$/.test(v)) return '한글/영문/숫자/_/-/공백만 사용할 수 있어요.';
    return null;
  };

  const checkAvailability = useMemo(
    () =>
      debounce(async (v: string) => {
        const fmtErr = validateFormat(v);
        if (fmtErr) {
          setError(fmtErr);
          setAvailable(null);
          setChecking(false);
          return;
        }
        setError(null);
        setChecking(true);
        try {
          // 서버에 맞춰 엔드포인트 이름만 맞추세요.
          // 예시: GET /api/users/check-nickname?nickname=xxx  -> { available: boolean }
          const res = await api.get<{ available: boolean }>(
            `/api/users/check-nickname?nickname=${encodeURIComponent(v)}`,
          );
          setAvailable(res.available);
        } catch {
          setAvailable(null);
          setError('중복 확인 중 오류가 발생했어요.');
        } finally {
          setChecking(false);
        }
      }, 400),
    [],
  );

  useEffect(() => {
    if (!open) return;
    if (!nick) {
      setAvailable(null);
      setError(null);
      return;
    }
    checkAvailability(nick);
  }, [nick, open, checkAvailability]);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const fmtErr = validateFormat(nick);
    if (fmtErr) return setError(fmtErr);
    if (!available) return setError('사용 가능한 닉네임을 입력해 주세요.');

    try {
      setSaving(true);
      // 서버에 맞춰 엔드포인트 이름만 맞추세요.
      // 예시: PATCH /api/users/me/nickname  body: { nickname }
      await api.post<void>('/api/users/me/nickname', { nickname: nick });
      onSaved(nick);
      onClose();
    } catch {
      setError('닉네임 저장에 실패했어요.');
    } finally {
      setSaving(false);
    }
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 bg-black/40 grid place-items-center z-50">
      <form onSubmit={onSubmit} className="w-[420px] rounded-2xl bg-white p-6 shadow-xl">
        <h2 className="text-xl font-bold mb-3">닉네임 설정</h2>
        <p className="text-sm text-gray-600 mb-4">서비스에서 사용할 공개 닉네임을 정해주세요.</p>

        <input
          autoFocus
          value={nick}
          onChange={(e) => setNick(e.target.value)}
          className="w-full border rounded-lg px-3 py-2 mb-2"
          placeholder="예) 코딩하는펭귄"
        />

        {checking && <div className="text-sm text-gray-500 mb-2">중복 확인 중…</div>}
        {available === true && (
          <div className="text-sm text-green-600 mb-2">사용 가능한 닉네임이에요!</div>
        )}
        {available === false && (
          <div className="text-sm text-red-600 mb-2">이미 사용 중인 닉네임이에요.</div>
        )}
        {error && <div className="text-sm text-red-600 mb-2">{error}</div>}

        <div className="flex justify-end gap-2 mt-2">
          <button type="button" onClick={onClose} className="px-3 py-2 rounded-lg border">
            취소
          </button>
          <button
            type="submit"
            disabled={saving || checking || !available}
            className="px-4 py-2 rounded-lg bg-indigo-600 text-white disabled:opacity-50"
          >
            {saving ? '저장 중…' : '저장'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default NicknameDialog;
