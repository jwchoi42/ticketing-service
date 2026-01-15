import React, { useEffect, useState } from 'react';
import { getMatches } from '../api/match';
import type { Match } from '../types';
import { useNavigate } from 'react-router-dom';

const MatchListPage: React.FC = () => {
    const [matches, setMatches] = useState<Match[]>([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchMatches = async () => {
            try {
                const data = await getMatches();
                setMatches(data || []);
            } catch (error) {
                console.error('Failed to fetch matches:', error);
            } finally {
                setLoading(false);
            }
        };
        fetchMatches();
    }, []);

    if (loading) return <div className="container">Loading matches...</div>;

    return (
        <div className="container">
            <h1 style={{ marginBottom: '2rem' }}>예매 가능한 경기</h1>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '1.5rem' }}>
                {matches.map((match) => (
                    <div key={match.id} className="card" style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                        <div style={{ fontSize: '1.25rem', fontWeight: 'bold' }}>
                            {match.homeTeam} vs {match.awayTeam}
                        </div>
                        <div style={{ color: 'var(--text-muted)' }}>
                            <div>📅 {new Date(match.dateTime).toLocaleString()}</div>
                            <div>🏟️ {match.stadium}</div>
                        </div>
                        <button
                            onClick={() => navigate(`/match/${match.id}`)}
                            style={{
                                marginTop: '1rem',
                                padding: '0.75rem',
                                backgroundColor: 'var(--primary-color)',
                                color: 'white',
                            }}
                        >
                            예매하기
                        </button>
                    </div>
                ))}
                {matches.length === 0 && <p>현재 진행 중인 경기가 없습니다.</p>}
            </div>
        </div>
    );
};

export default MatchListPage;
