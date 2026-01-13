import React from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Header: React.FC = () => {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    return (
        <header style={{
            backgroundColor: 'var(--bg-card)',
            padding: '1rem 2rem',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            borderBottom: '1px solid #334155'
        }}>
            <Link to="/" style={{ color: 'white', textDecoration: 'none', fontSize: '1.5rem', fontWeight: 'bold' }}>
                Ticketing Service
            </Link>
            {user && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
                    <span style={{ color: 'var(--text-muted)' }}>{user.email}님 환영합니다</span>
                    <button
                        onClick={handleLogout}
                        style={{
                            padding: '0.5rem 1rem',
                            backgroundColor: 'transparent',
                            color: 'var(--error)',
                            border: '1px solid var(--error)'
                        }}
                    >
                        로그아웃
                    </button>
                </div>
            )}
        </header>
    );
};

export default Header;
