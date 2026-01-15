import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { logIn, signUp } from '../api/user';
import { useNavigate } from 'react-router-dom';

const LoginPage: React.FC = () => {
    const [email, setEmail] = useState('');
    const [isLogin, setIsLogin] = useState(true);
    const { login } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const user = isLogin ? await logIn(email) : await signUp(email);
            login(user);
            navigate('/');
        } catch (error: any) {
            console.error('Auth error:', error);
            const message = error.response?.data?.data?.message || error.response?.data?.message || '인증에 실패했습니다. 서버 상태를 확인해주세요.';
            alert(message);
        }
    };

    return (
        <div className="container" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh' }}>
            <div className="card" style={{ width: '400px' }}>
                <h2 style={{ marginBottom: '1.5rem', textAlign: 'center' }}>
                    {isLogin ? '로그인' : '회원가입'}
                </h2>
                <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    <input
                        type="email"
                        placeholder="이메일을 입력하세요"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                    />
                    <button type="submit" style={{ padding: '0.75rem', backgroundColor: 'var(--primary-color)', color: 'white' }}>
                        {isLogin ? '로그인' : '회원가입'}
                    </button>
                </form>
                <p style={{ marginTop: '1rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                    {isLogin ? '계정이 없으신가요?' : '이미 계정이 있으신가요?'}
                    <span
                        onClick={() => setIsLogin(!isLogin)}
                        style={{ color: 'var(--primary-color)', marginLeft: '0.5rem', cursor: 'pointer' }}
                    >
                        {isLogin ? '회원가입' : '로그인'}
                    </span>
                </p>
            </div>
        </div>
    );
};

export default LoginPage;
