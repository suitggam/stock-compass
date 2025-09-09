import { useSearchParams, Link } from 'react-router-dom';

export default function OAuthFail() {
  const [sp] = useSearchParams();
  return (
    <div style={{ padding: 24 }}>
      <h2>로그인에 실패했어요 😢</h2>
      <p>사유: {sp.get('reason') ?? '알 수 없음'}</p>
      <Link to="/">홈으로</Link>
    </div>
  );
}
