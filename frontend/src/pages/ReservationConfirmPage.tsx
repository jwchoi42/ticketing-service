import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { createReservation, requestPayment, confirmPayment } from '../api/reservation';

const ReservationConfirmPage: React.FC = () => {
    const { state } = useLocation() as { state: { matchId: number, seatIds: number[] } };
    const { user } = useAuth();
    const { showToast } = useToast();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [reservationId, setReservationId] = useState<number | null>(null);

    if (!state || !user) {
        return <div className="container">잘못된 접근입니다.</div>;
    }

    const handleCreateReservation = async () => {
        setLoading(true);
        try {
            const res = await createReservation({
                matchId: state.matchId,
                userId: user.id,
                seatIds: state.seatIds
            });
            setReservationId(res.id);
            showToast('예약이 생성되었습니다. 결제를 진행해 주세요.', 'success');
        } catch (error) {
            showToast('예약 생성에 실패했습니다.', 'error');
        } finally {
            setLoading(false);
        }
    };

    const handlePayment = async () => {
        if (!reservationId) return;
        setLoading(true);
        try {
            // Calculate dummy amount (e.g., 10,000 KRW per seat)
            const amount = state.seatIds.length * 10000;

            // 1. Request Payment
            const payment = await requestPayment(reservationId, amount);

            // 2. Confirm Payment
            await confirmPayment(payment.id, amount);

            showToast('결제가 완료되었습니다!', 'success');
            navigate('/');
        } catch (error) {
            console.error(error);
            showToast('결제 처리에 실패했습니다.', 'error');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="container">
            <div className="card" style={{ maxWidth: '600px', margin: '0 auto' }}>
                <h2 style={{ marginBottom: '1.5rem' }}>예약 확인</h2>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginBottom: '2rem' }}>
                    <div style={{ color: 'var(--text-muted)' }}>경기 ID: {state.matchId}</div>
                    <div style={{ fontSize: '1.25rem' }}>
                        선택된 좌석 수: <span style={{ color: 'var(--secondary-color)', fontWeight: 'bold' }}>{state.seatIds.length}</span>개
                    </div>
                </div>

                {!reservationId ? (
                    <button
                        onClick={handleCreateReservation}
                        disabled={loading}
                        style={{
                            width: '100%',
                            padding: '1rem',
                            backgroundColor: 'var(--primary-color)',
                            color: 'white',
                            fontSize: '1.125rem'
                        }}
                    >
                        {loading ? '처리 중...' : '예약 생성하기'}
                    </button>
                ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                        <div style={{ padding: '1rem', backgroundColor: '#334155', borderRadius: '8px', textAlign: 'center' }}>
                            예약 번호: <strong>{reservationId}</strong>
                        </div>
                        <button
                            onClick={handlePayment}
                            disabled={loading}
                            style={{
                                width: '100%',
                                padding: '1rem',
                                backgroundColor: 'var(--success)',
                                color: 'white',
                                fontSize: '1.125rem'
                            }}
                        >
                            {loading ? '결제 중...' : '결제하기'}
                        </button>
                    </div>
                )}

                <button
                    onClick={() => navigate(-1)}
                    style={{
                        width: '100%',
                        marginTop: '1rem',
                        padding: '1rem',
                        backgroundColor: 'transparent',
                        color: 'var(--text-muted)',
                        border: '1px solid #475569'
                    }}
                >
                    뒤로 가기
                </button>
            </div>
        </div>
    );
};

export default ReservationConfirmPage;
