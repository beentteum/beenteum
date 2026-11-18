import { BrowserRouter, Link, Route, Routes } from 'react-router'

function HomePage() {
  return (
    <main>
      <h1>빈틈</h1>
      <p>카페 혼잡도 정보 서비스</p>
      <p>샘플 카페 · 여유</p>
      <Link to="/login">로그인 화면으로</Link>
    </main>
  )
}

function LoginPage() {
  return (
    <main>
      <h1>로그인</h1>
      <Link to="/">HOME으로 돌아가기</Link>
    </main>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
      </Routes>
    </BrowserRouter>
  )
}