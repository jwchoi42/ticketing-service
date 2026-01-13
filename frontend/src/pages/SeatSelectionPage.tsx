import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getAreas, getSections, getBlocks, getSeats } from '../api/site';
import type { Area, Section, Block, Seat, SeatStatusChange } from '../types';
import { useSSE } from '../hooks/useSSE';
import { holdSeat, releaseSeat, completeAllocation } from '../api/allocation';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

const SeatSelectionPage: React.FC = () => {
    const { matchId } = useParams<{ matchId: string }>();
    const id = Number(matchId);
    const { user } = useAuth();
    const { showToast } = useToast();
    const navigate = useNavigate();

    const [areas, setAreas] = useState<Area[]>([]);
    const [sections, setSections] = useState<Section[]>([]);
    const [blocks, setBlocks] = useState<Block[]>([]);
    const [seats, setSeats] = useState<Seat[]>([]);
    const [heldSeatIds, setHeldSeatIds] = useState<number[]>([]); // Current user's held seats

    const [selectedArea, setSelectedArea] = useState<number | null>(null);
    const [selectedSection, setSelectedSection] = useState<number | null>(null);
    const [selectedBlock, setSelectedBlock] = useState<number | null>(null);

    // SSE Integration
    const { status: sseStatus } = useSSE(id, selectedBlock, {
        onSnapshot: (initialStatuses: Seat[]) => {
            setSeats(prev => prev.map(seat => {
                const statusInfo = initialStatuses.find(s => s.id === seat.id);
                return statusInfo ? { ...seat, status: statusInfo.status } : seat;
            }));
        },
        onChanges: (changes: SeatStatusChange[]) => {
            setSeats(prev => prev.map(seat => {
                const change = changes.find(c => c.seatId === seat.id);
                return change ? { ...seat, status: change.status } : seat;
            }));
        },
        onError: () => {
            showToast('실시간 연결에 문제가 발생했습니다. 다시 시도해 주세요.', 'error');
        }
    });

    useEffect(() => {
        getAreas().then(setAreas);
    }, []);

    useEffect(() => {
        if (selectedArea) {
            getSections(selectedArea).then(setSections);
            setSelectedSection(null);
            setSelectedBlock(null);
            setBlocks([]);
            setSeats([]);
        }
    }, [selectedArea]);

    useEffect(() => {
        if (selectedSection) {
            getBlocks(selectedSection).then(setBlocks);
            setSelectedBlock(null);
            setSeats([]);
        }
    }, [selectedSection]);

    useEffect(() => {
        if (selectedBlock) {
            getSeats(selectedBlock).then(data => {
                // Map hierarchy data (id, rowNumber, seatNumber) to frontend Seat type
                const mappedSeats: Seat[] = data.map((s: any) => ({
                    id: s.id,
                    seatNumber: `${s.rowNumber}-${s.seatNumber}`,
                    status: 'AVAILABLE' // Default status before snapshot
                }));
                setSeats(mappedSeats);
            });
        } else {
            setSeats([]);
        }
    }, [selectedBlock]);

    const handleSeatClick = async (seat: Seat) => {
        if (!user) return;

        const isHeldByMe = heldSeatIds.includes(seat.id);

        if (seat.status === 'AVAILABLE') {
            // Hold seat
            try {
                // Optimistic UI
                setSeats(prev => prev.map(s => s.id === seat.id ? { ...s, status: 'HOLD' } : s));
                setHeldSeatIds(prev => [...prev, seat.id]);

                await holdSeat(id, seat.id, user.id);
                showToast(`좌석 ${seat.seatNumber}번을 선택했습니다.`, 'success');
            } catch (error) {
                // Rollback
                setSeats(prev => prev.map(s => s.id === seat.id ? { ...s, status: 'AVAILABLE' } : s));
                setHeldSeatIds(prev => prev.filter(id => id !== seat.id));
                showToast('좌석 선택에 실패했습니다. 이미 선택된 좌석일 수 있습니다.', 'error');
            }
        } else if (seat.status === 'HOLD' && isHeldByMe) {
            // Release seat
            try {
                // Optimistic UI
                setSeats(prev => prev.map(s => s.id === seat.id ? { ...s, status: 'AVAILABLE' } : s));
                setHeldSeatIds(prev => prev.filter(id => id !== seat.id));

                await releaseSeat(id, seat.id, user.id);
                showToast(`좌석 ${seat.seatNumber}번 선택을 해제했습니다.`, 'info');
            } catch (error) {
                // Rollback (Keep it HOLD if release failed)
                setSeats(prev => prev.map(s => s.id === seat.id ? { ...s, status: 'HOLD' } : s));
                setHeldSeatIds(prev => [...prev, seat.id]);
                showToast('좌석 해제에 실패했습니다.', 'error');
            }
        }
    };

    const handleComplete = async () => {
        if (!user || heldSeatIds.length === 0) return;

        // 백엔드 confirmTickets API 미구현으로 인해 호출 생략
        // 바로 예약 확인 페이지로 이동하여 실제 예약(Reservation) 생성 시 검증
        navigate(`/reservation/confirm`, { state: { matchId: id, seatIds: heldSeatIds } });
    };

    return (
        <div className="container">
            <h1>좌석 선택</h1>

            {/* Navigation Hierarchy */}
            <div style={{ display: 'flex', gap: '1rem', margin: '2rem 0' }}>
                <div className="card" style={{ flex: 1 }}>
                    <h3>권역 (Area)</h3>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginTop: '1rem' }}>
                        {areas.map(a => (
                            <button
                                key={a.id}
                                className={selectedArea === a.id ? 'active' : ''}
                                onClick={() => setSelectedArea(a.id)}
                                style={{
                                    padding: '0.5rem',
                                    backgroundColor: selectedArea === a.id ? 'var(--primary-color)' : '#334155',
                                    color: 'white'
                                }}
                            >
                                {a.name}
                            </button>
                        ))}
                    </div>
                </div>

                <div className="card" style={{ flex: 1 }}>
                    <h3>진영 (Section)</h3>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginTop: '1rem' }}>
                        {sections.map(s => (
                            <button
                                key={s.id}
                                onClick={() => setSelectedSection(s.id)}
                                style={{
                                    padding: '0.5rem',
                                    backgroundColor: selectedSection === s.id ? 'var(--primary-color)' : '#334155',
                                    color: 'white'
                                }}
                            >
                                {s.name}
                            </button>
                        ))}
                        {sections.length === 0 && <p style={{ color: 'var(--text-muted)' }}>권역을 먼저 선택하세요</p>}
                    </div>
                </div>

                <div className="card" style={{ flex: 1 }}>
                    <h3>구역 (Block)</h3>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginTop: '1rem' }}>
                        {blocks.map(b => (
                            <button
                                key={b.id}
                                onClick={() => setSelectedBlock(b.id)}
                                style={{
                                    padding: '0.5rem',
                                    backgroundColor: selectedBlock === b.id ? 'var(--primary-color)' : '#334155',
                                    color: 'white'
                                }}
                            >
                                {b.name}
                            </button>
                        ))}
                        {blocks.length === 0 && <p style={{ color: 'var(--text-muted)' }}>진영을 먼저 선택하세요</p>}
                    </div>
                </div>
            </div>

            {/* SSE Status Indicator */}
            {selectedBlock && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
                    <div style={{
                        width: '10px',
                        height: '10px',
                        borderRadius: '50%',
                        backgroundColor: sseStatus === 'connected' ? 'var(--success)' :
                            sseStatus === 'reconnecting' ? 'var(--hold)' : 'var(--error)'
                    }}></div>
                    <span style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
                        {sseStatus === 'connected' ? '실시간 연결 중' :
                            sseStatus === 'reconnecting' ? '재연결 시도 중...' : '연결 끊김'}
                    </span>
                </div>
            )}

            {/* Seat Grid */}
            {selectedBlock && (
                <div className="card">
                    <h3>좌석 배치도 - {blocks.find(b => b.id === selectedBlock)?.name}</h3>
                    <div style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(10, 1fr)',
                        gap: '10px',
                        marginTop: '2rem',
                        maxWidth: '600px',
                        margin: '2rem auto'
                    }}>
                        {seats.map(seat => {
                            const isHeldByMe = heldSeatIds.includes(seat.id);
                            return (
                                <div
                                    key={seat.id}
                                    onClick={() => handleSeatClick(seat)}
                                    style={{
                                        aspectRatio: '1/1',
                                        backgroundColor: seat.status === 'AVAILABLE' ? 'var(--available)' :
                                            (seat.status === 'HOLD' && isHeldByMe) ? 'var(--hold)' :
                                                seat.status === 'HOLD' ? '#4a5568' : 'var(--occupied)',
                                        display: 'flex',
                                        justifyContent: 'center',
                                        alignItems: 'center',
                                        borderRadius: '4px',
                                        cursor: (seat.status === 'AVAILABLE' || (seat.status === 'HOLD' && isHeldByMe)) ? 'pointer' : 'not-allowed',
                                        fontSize: '0.75rem',
                                        fontWeight: 'bold',
                                        border: isHeldByMe ? '2px solid white' : 'none',
                                        position: 'relative'
                                    }}
                                    title={`Seat ${seat.seatNumber} (${seat.status})`}
                                >
                                    {seat.seatNumber}
                                    {isHeldByMe && (
                                        <span style={{ position: 'absolute', top: '-5px', right: '-5px', fontSize: '10px' }}>⭐</span>
                                    )}
                                </div>
                            );
                        })}
                    </div>

                    <div style={{ marginTop: '2rem', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '1rem' }}>
                        <div style={{ fontSize: '1.125rem' }}>
                            선택된 좌석: <span style={{ color: 'var(--secondary-color)', fontWeight: 'bold' }}>{heldSeatIds.length}</span>개
                        </div>
                        <button
                            onClick={handleComplete}
                            disabled={heldSeatIds.length === 0}
                            style={{
                                padding: '1rem 2rem',
                                backgroundColor: heldSeatIds.length > 0 ? 'var(--primary-color)' : '#475569',
                                color: 'white',
                                fontSize: '1.125rem',
                                width: '100%',
                                maxWidth: '300px'
                            }}
                        >
                            선택 완료
                        </button>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'center', gap: '2rem', marginTop: '2rem' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            <div style={{ width: '20px', height: '20px', backgroundColor: 'var(--available)', borderRadius: '4px' }}></div>
                            <span>선택 가능</span>
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            <div style={{ width: '20px', height: '20px', backgroundColor: 'var(--hold)', borderRadius: '4px' }}></div>
                            <span>선택됨(Hold)</span>
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            <div style={{ width: '20px', height: '20px', backgroundColor: 'var(--occupied)', borderRadius: '4px' }}></div>
                            <span>판매 완료</span>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default SeatSelectionPage;
