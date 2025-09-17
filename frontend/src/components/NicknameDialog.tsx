import React, { useEffect, useState } from 'react';
import { api } from '../api/client';

type Props = {
  open: boolean;
  initialNickname?: string;
  onClose: () => void;
  onSaved: (newNickname: string) => void;
};

// 백엔드: PATCH /users/me  (body: { nickname })
// 응답: UserSummaryDto(여기선 nickname만 사용)
type UserSummary = { nickname: string; [k: string]: unknown };

// axios류 에러에서 메시지 뽑기 (any 금지)
type ApiError = { response?: { data?: { message?: string; error?: string } } };
const extractMsg = (err: unknown) => {
  if (typeof err === 'object' && err !== null) {
    const e = err as ApiError;
    return e.response?.data?.message ?? e.response?.data?.error;
  }
  return undefined;
};

const NicknameDialog: React.FC<Props> = ({ open, initialNickname, onClose, onSaved }) => {
  const [nick, setNick] = useState(initialNickname ?? '');
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setNick(initialNickname ?? '');
    setError(null);
  }, [open, initialNickname]);

  const validateFormat = (v: string) => {
    if (!v || v.trim().length < 2) return '닉네임은 2자 이상이어야 해요.';
    if (v.length > 30) return '닉네임은 최대 30자까지 가능합니다.';
    if (!/^[\w가-힣\- ]+$/.test(v)) return '한글/영문/숫자/_/-/공백만 사용할 수 있어요.';
    return null;
  };

  useEffect(() => {
    if (!open) return;
    if (!nick) {
      setError(null);
      return;
    }
    setError(validateFormat(nick));
  }, [nick, open]);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const fmtErr = validateFormat(nick);
    if (fmtErr) return setError(fmtErr);

    try {
      setSaving(true);
      const body = { nickname: nick.trim() };
      // 백엔드 스펙: PATCH /users/me -> UserSummaryDto
      const res = await api.patch<UserSummary>('/users/me', body);
      onSaved(res.nickname ?? body.nickname);
      onClose();
    } catch (err: unknown) {
      const msg = extractMsg(err) ?? '닉네임 저장에 실패했어요.';
      setError(msg);
    } finally {
      setSaving(false);
    }
  };

  if (!open) return null;

  const canSubmit = !saving && !error && nick.trim().length >= 2 && nick.trim().length <= 30;

  return (
    <div className="fixed inset-0 bg-black/40 grid place-items-center z-50">
      <form onSubmit={onSubmit} className="w-[420px] rounded-2xl bg-white p-6 shadow-xl">
        <h2 className="text-xl font-bold mb-3">닉네임 설정</h2>
        <p className="text-sm text-gray-600 mb-4">서비스에서 사용할 닉네임을 정해주세요.</p>

        <input
          autoFocus
          value={nick}
          onChange={(e) => setNick(e.target.value)}
          className="w-full border rounded-lg px-3 py-2 mb-2"
          placeholder="예) 테토보이즈리더"
        />

        {!error && nick && (
          <div className="text-sm text-green-600 mb-2">사용 가능한 닉네임입니다.</div>
        )}
        {error && <div className="text-sm text-red-600 mb-2">{error}</div>}

        <div className="flex justify-end gap-2 mt-2">
          <button type="button" onClick={onClose} className="px-3 py-2 rounded-lg border">
            취소
          </button>
          <button
            type="submit"
            disabled={!canSubmit}
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
