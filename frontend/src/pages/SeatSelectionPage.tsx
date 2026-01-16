import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getAreas, getSections, getBlocks, getSeats } from '../api/site';
import type { Area, Section, Block, Seat, SeatStatusChange } from '../types';
import { useSSE, type ConnectionStatus } from '../hooks/useSSE';
import { holdSeat, releaseSeat } from '../api/allocation';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

interface HeldSeat extends Seat {
    blockName: string;
}

const SeatSelectionPage: React.FC = () => {
    const { matchId } = useParams<{ matchId: string }>();
    const id = Number(matchId);
    const { user } = useAuth();
    const { showToast } = useToast();
    const navigate = useNavigate();

    const [areas, setAreas] = useState<Area[]>([]);
    const [areaSections, setAreaSections] = useState<Record<number, Section[]>>({});
    const [sectionBlocks, setSectionBlocks] = useState<Record<number, Block[]>>({});

    const [expandedAreas, setExpandedAreas] = useState<Set<number>>(() => {
        const saved = localStorage.getItem(`expandedAreas_${id}`);
        return saved ? new Set(JSON.parse(saved)) : new Set();
    });
    const [selectedBlock, setSelectedBlock] = useState<number | null>(() => {
        const saved = localStorage.getItem(`selectedBlock_${id}`);
        return saved ? Number(saved) : null;
    });
    const [selectedSection, setSelectedSection] = useState<number | null>(() => {
        const saved = localStorage.getItem(`selectedSection_${id}`);
        return saved ? Number(saved) : null;
    });
    const [lastClicked, setLastClicked] = useState<{ id: number, type: 'area' | 'section' } | null>(() => {
        const saved = localStorage.getItem(`lastClicked_${id}`);
        return saved ? JSON.parse(saved) : null;
    });

    const [seats, setSeats] = useState<Seat[]>([]);
    const [blocksLoading, setBlocksLoading] = useState(false);
    const [seatsLoading, setSeatsLoading] = useState(false);

    // Persistence effects
    useEffect(() => {
        localStorage.setItem(`expandedAreas_${id}`, JSON.stringify(Array.from(expandedAreas)));
    }, [expandedAreas, id]);
    useEffect(() => {
        if (selectedBlock) localStorage.setItem(`selectedBlock_${id}`, selectedBlock.toString());
        else localStorage.removeItem(`selectedBlock_${id}`);
    }, [selectedBlock, id]);
    useEffect(() => {
        if (selectedSection) localStorage.setItem(`selectedSection_${id}`, selectedSection.toString());
        else localStorage.removeItem(`selectedSection_${id}`);
    }, [selectedSection, id]);
    useEffect(() => {
        if (lastClicked) localStorage.setItem(`lastClicked_${id}`, JSON.stringify(lastClicked));
        else localStorage.removeItem(`lastClicked_${id}`);
    }, [lastClicked, id]);

    // Initializing heldSeats from localStorage to persist across refreshes
    const [heldSeats, setHeldSeats] = useState<HeldSeat[]>(() => {
        const saved = localStorage.getItem(`heldSeatsV2_${id}_${user?.id}`);
        return saved ? JSON.parse(saved) : [];
    });

    // Update localStorage whenever heldSeats changes
    useEffect(() => {
        if (user) {
            localStorage.setItem(`heldSeatsV2_${id}_${user.id}`, JSON.stringify(heldSeats));
        }
    }, [heldSeats, id, user]);

    // SSE Integration
    const { status: sseStatus, retryCount, maxRetries } = useSSE(id, selectedBlock, {
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
        }
    });

    const isSseConnecting = sseStatus === 'connecting' || sseStatus === 'reconnecting';

    useEffect(() => {
        getAreas().then(async (areaData) => {
            setAreas(areaData);
            // Pre-fetch sections for expanded areas on refresh
            const areaSectionsMap: Record<number, Section[]> = {};
            for (const areaId of Array.from(expandedAreas)) {
                if (!areaSections[areaId]) {
                    const sections = await getSections(areaId);
                    areaSectionsMap[areaId] = sections;
                }
            }
            if (Object.keys(areaSectionsMap).length > 0) {
                setAreaSections(prev => ({ ...prev, ...areaSectionsMap }));
            }

            // Pre-fetch blocks for selected section on refresh
            if (selectedSection && !sectionBlocks[selectedSection]) {
                const blocks = await getBlocks(selectedSection);
                setSectionBlocks(prev => ({ ...prev, [selectedSection]: blocks }));
            }
        });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const toggleArea = async (areaId: number) => {
        const next = new Set(expandedAreas);
        if (next.has(areaId)) {
            next.delete(areaId);
        } else {
            next.add(areaId);
            if (!areaSections[areaId]) {
                const data = await getSections(areaId);
                setAreaSections(prev => ({ ...prev, [areaId]: data }));
            }
        }
        setExpandedAreas(next);
        setLastClicked({ id: areaId, type: 'area' });
    };

    const toggleSection = async (sectionId: number) => {
        setSelectedSection(sectionId);
        setLastClicked({ id: sectionId, type: 'section' });

        // Immediately reset selectedBlock to show loading state if needed
        setSelectedBlock(null);

        if (!sectionBlocks[sectionId]) {
            setBlocksLoading(true);
            try {
                const data = await getBlocks(sectionId);
                setSectionBlocks(prev => ({ ...prev, [sectionId]: data }));
                if (data.length > 0) {
                    setSelectedBlock(data[0].id);
                }
            } finally {
                setBlocksLoading(false);
            }
        } else if (sectionBlocks[sectionId].length > 0) {
            setSelectedBlock(sectionBlocks[sectionId][0].id);
        }
    };

    const handleNextBlock = () => {
        if (!selectedSection || !selectedBlock || !sectionBlocks[selectedSection]) return;
        const blocks = sectionBlocks[selectedSection];
        const currentIndex = blocks.findIndex(b => b.id === selectedBlock);
        if (currentIndex < blocks.length - 1) {
            setSelectedBlock(blocks[currentIndex + 1].id);
        }
    };

    const handlePrevBlock = () => {
        if (!selectedSection || !selectedBlock || !sectionBlocks[selectedSection]) return;
        const blocks = sectionBlocks[selectedSection];
        const currentIndex = blocks.findIndex(b => b.id === selectedBlock);
        if (currentIndex > 0) {
            setSelectedBlock(blocks[currentIndex - 1].id);
        }
    };

    useEffect(() => {
        if (selectedBlock) {
            setSeatsLoading(true);
            getSeats(selectedBlock).then(data => {
                const mappedSeats: Seat[] = data.map((s) => ({
                    id: s.id,
                    rowNumber: s.rowNumber,
                    seatNumber: s.seatNumber,
                    status: 'AVAILABLE'
                }));
                setSeats(mappedSeats);
            }).finally(() => {
                setSeatsLoading(false);
            });
        } else {
            setSeats([]);
        }
    }, [selectedBlock]);

    const getBlockInfo = (blockId: number) => {
        if (!selectedSection || !sectionBlocks[selectedSection]) return null;
        const blocks = sectionBlocks[selectedSection];
        const index = blocks.findIndex(b => b.id === blockId);
        if (index === -1) return null;
        return {
            name: blocks[index].name,
            hasPrev: index > 0,
            hasNext: index < blocks.length - 1
        };
    };

    const blockInfo = selectedBlock ? getBlockInfo(selectedBlock) : null;

    const handleSeatClick = async (seat: Seat) => {
        if (!user) return;

        const isHeldByMe = heldSeats.some(h => h.id === seat.id);

        if (seat.status === 'AVAILABLE') {
            try {
                setSeats(prev => prev.map(s => s.id === seat.id ? { ...s, status: 'HOLD' } : s));

                const newHeldSeat: HeldSeat = { ...seat, blockName: blockInfo?.name || '' };
                setHeldSeats(prev => [...prev, newHeldSeat]);

                await holdSeat(id, seat.id, user.id);
                showToast(`좌석 ${seat.rowNumber}행 ${seat.seatNumber}열을 선택했습니다.`, 'success');
            } catch {
                setSeats(prev => prev.map(s => s.id === seat.id ? { ...s, status: 'AVAILABLE' } : s));
                setHeldSeats(prev => prev.filter(h => h.id !== seat.id));
                showToast('좌석 선택에 실패했습니다. 이미 선택된 좌석일 수 있습니다.', 'error');
            }
        } else if (seat.status === 'HOLD' && isHeldByMe) {
            try {
                setSeats(prev => prev.map(s => s.id === seat.id ? { ...s, status: 'AVAILABLE' } : s));
                setHeldSeats(prev => prev.filter(h => h.id !== seat.id));

                await releaseSeat(id, seat.id, user.id);
                showToast(`좌석 ${seat.rowNumber}행 ${seat.seatNumber}열 선택을 해제했습니다.`, 'info');
            } catch {
                setSeats(prev => prev.map(s => s.id === seat.id ? { ...s, status: 'HOLD' } : s));
                if (!heldSeats.some(h => h.id === seat.id)) {
                    const newHeldSeat: HeldSeat = { ...seat, blockName: blockInfo?.name || '' };
                    setHeldSeats(prev => [...prev, newHeldSeat]);
                }
                showToast('좌석 해제에 실패했습니다.', 'error');
            }
        }
    };

    const handleComplete = async () => {
        if (!user || heldSeats.length === 0) return;
        navigate(`/reservation/confirm`, { state: { matchId: id, seatIds: heldSeats.map(h => h.id) } });
    };

    const rowNumbers = Array.from(new Set(seats.map(s => s.rowNumber))).sort((a, b) => a - b);
    const colNumbers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];


    return (
        <div className="container">
            <h1 style={{ marginBottom: '2rem' }}>좌석 선택</h1>

            {/* SSE Definitive Error Popup */}
            {sseStatus === 'error' && (
                <div style={{
                    position: 'fixed', top: 0, left: 0, width: '100%', height: '100%',
                    backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 1000,
                    display: 'flex', justifyContent: 'center', alignItems: 'center'
                }}>
                    <div className="card" style={{ maxWidth: '400px', textAlign: 'center', padding: '2rem' }}>
                        <h3 style={{ color: 'var(--error)', marginBottom: '1rem' }}>연결 실패</h3>
                        <p style={{ marginBottom: '2rem' }}>실시간 좌석 정보를 불러올 수 없습니다. {maxRetries}회 재시도했으나 실패했습니다.</p>
                        <button
                            onClick={() => window.location.reload()}
                            style={{ backgroundColor: 'var(--primary-color)', color: 'white', padding: '0.75rem 1.5rem', width: '100%' }}
                        >
                            페이지 새로고침
                        </button>
                    </div>
                </div>
            )}

            <div className="reservation-layout">
                {/* Left Sidebar */}
                <aside className="sidebar card">
                    <h3 style={{ marginBottom: '1rem', fontSize: '1.125rem' }}>영역 탐색</h3>
                    <div className="tree-container">
                        {areas.map(area => (
                            <div key={area.id}>
                                <div
                                    className={`tree-item ${lastClicked?.id === area.id && lastClicked?.type === 'area' ? 'active' : ''}`}
                                    onClick={() => toggleArea(area.id)}
                                >
                                    <span className="tree-icon" style={{ cursor: 'pointer' }}>{expandedAreas.has(area.id) ? '▼' : '▶'}</span>
                                    {area.name}
                                </div>

                                {expandedAreas.has(area.id) && areaSections[area.id] && (
                                    <div className="tree-node-children">
                                        {areaSections[area.id].map(section => (
                                            <div key={section.id}>
                                                <div
                                                    className={`tree-item ${lastClicked?.id === section.id && lastClicked?.type === 'section' ? 'active' : ''}`}
                                                    onClick={() => toggleSection(section.id)}
                                                >
                                                    {section.name}
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>
                </aside>

                {/* Main Content (Seat Map) */}
                <main className="main-content">
                    {/* SSE & Data Loading State */}
                    {(blocksLoading || seatsLoading || isSseConnecting) ? (
                        <div className="card" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', minHeight: '400px' }}>
                            <div className="loader" style={{
                                width: '40px', height: '40px', border: '4px solid #f3f3f3',
                                borderTop: '4px solid var(--primary-color)', borderRadius: '50%',
                                animation: 'spin 1s linear infinite', marginBottom: '1rem'
                            }}></div>
                            <style>{`@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`}</style>
                            <span style={{ color: 'var(--text-muted)' }}>
                                {blocksLoading ? '구역 정보를 불러오는 중...' :
                                    seatsLoading ? '좌석 배치도를 불러오는 중...' :
                                        sseStatus === 'reconnecting' ? '재연결 시도 중...' : '실시간 연결 중...'}
                            </span>
                        </div>
                    ) : (selectedBlock && blockInfo) ? (
                        <div className="card">
                            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '2rem', marginBottom: '1rem' }}>
                                <button
                                    onClick={handlePrevBlock}
                                    disabled={!blockInfo.hasPrev}
                                    style={{
                                        width: '40px', height: '40px', padding: 0, display: 'flex', alignItems: 'center', justifyContent: 'center',
                                        backgroundColor: blockInfo.hasPrev ? 'var(--primary-color)' : '#e2e8f0',
                                        color: 'white', borderRadius: '50%', fontSize: '1.25rem', cursor: blockInfo.hasPrev ? 'pointer' : 'not-allowed'
                                    }}
                                >
                                    &larr;
                                </button>
                                <h3 style={{ margin: 0, minWidth: '200px', textAlign: 'center' }}>
                                    좌석 배치도 - {blockInfo.name}
                                </h3>
                                <button
                                    onClick={handleNextBlock}
                                    disabled={!blockInfo.hasNext}
                                    style={{
                                        width: '40px', height: '40px', padding: 0, display: 'flex', alignItems: 'center', justifyContent: 'center',
                                        backgroundColor: blockInfo.hasNext ? 'var(--primary-color)' : '#e2e8f0',
                                        color: 'white', borderRadius: '50%', fontSize: '1.25rem', cursor: blockInfo.hasNext ? 'pointer' : 'not-allowed'
                                    }}
                                >
                                    &rarr;
                                </button>
                            </div>

                            {/* SSE Status Mini Indicator */}
                            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '0.4rem', marginBottom: '2rem' }}>
                                <div style={{
                                    width: '8px', height: '8px', borderRadius: '50%',
                                    backgroundColor: (sseStatus as ConnectionStatus) === 'connected' ? 'var(--success)' :
                                        ((sseStatus as ConnectionStatus) === 'reconnecting' || (sseStatus as ConnectionStatus) === 'connecting') ? 'var(--hold)' : 'var(--error)'
                                }}></div>
                                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                                    {(sseStatus as ConnectionStatus) === 'connected' ? '실시간 연결됨' :
                                        (sseStatus as ConnectionStatus) === 'reconnecting' ? `재연결 시도 중 (${retryCount}/${maxRetries})` :
                                            (sseStatus as ConnectionStatus) === 'connecting' ? '연결 중...' : '연결 끊김'}
                                </span>
                            </div>

                            <div style={{ overflowX: 'auto', padding: '1rem', marginBottom: '2rem' }}>
                                <div style={{
                                    display: 'grid',
                                    gridTemplateColumns: `50px repeat(${colNumbers.length}, 35px)`,
                                    gap: '8px', justifyContent: 'center', minWidth: 'fit-content'
                                }}>
                                    <div />
                                    {colNumbers.map(col => (
                                        <div key={`col-${col}`} style={{ textAlign: 'center', fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: '600' }}>
                                            {col}
                                        </div>
                                    ))}

                                    {rowNumbers.map(row => (
                                        <React.Fragment key={`row-${row}`}>
                                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.75rem', fontWeight: '600', color: 'var(--text-muted)' }}>
                                                {row}행
                                            </div>
                                            {colNumbers.map(col => {
                                                const seat = seats.find(s => s.rowNumber === row && ((s.seatNumber - 1) % 10 + 1) === col);
                                                if (!seat) return <div key={`empty-${row}-${col}`} />;

                                                const isHeldByMe = heldSeats.some(h => h.id === seat.id);
                                                const bgColor = seat.status === 'AVAILABLE' ? 'var(--available)' :
                                                    (seat.status === 'HOLD' && isHeldByMe) ? 'var(--selected)' :
                                                        seat.status === 'HOLD' ? 'var(--hold)' : 'var(--occupied)';
                                                const cursor = (seat.status === 'AVAILABLE' || (seat.status === 'HOLD' && isHeldByMe)) ? 'pointer' : 'not-allowed';

                                                return (
                                                    <div
                                                        key={seat.id}
                                                        onClick={() => handleSeatClick(seat)}
                                                        style={{
                                                            width: '35px', height: '35px', backgroundColor: bgColor,
                                                            display: 'flex', justifyContent: 'center', alignItems: 'center',
                                                            borderRadius: '6px', cursor: cursor, fontSize: '0.625rem', fontWeight: 'bold',
                                                            border: isHeldByMe ? '2px solid var(--primary-color)' : '1px solid rgba(0,0,0,0.05)',
                                                            transition: 'all 0.2s ease', position: 'relative'
                                                        }}
                                                        title={`${row}행 ${col}열 (${seat.status})`}
                                                    >
                                                        {isHeldByMe && (
                                                            <span style={{ position: 'absolute', top: '-6px', right: '-6px', fontSize: '12px' }}>✅</span>
                                                        )}
                                                    </div>
                                                );
                                            })}
                                        </React.Fragment>
                                    ))}
                                </div>
                            </div>

                            <div style={{ marginTop: '2rem', borderTop: '1px solid #e2e8f0', paddingTop: '2rem' }}>
                                <div style={{ display: 'flex', justifyContent: 'center', gap: '1.5rem', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                        <div style={{ width: '20px', height: '20px', backgroundColor: 'var(--available)', borderRadius: '4px' }}></div>
                                        <span style={{ fontSize: '0.875rem' }}>선택 가능</span>
                                    </div>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                        <div style={{ width: '20px', height: '20px', backgroundColor: 'var(--selected)', borderRadius: '4px' }}></div>
                                        <span style={{ fontSize: '0.875rem' }}>나의 선택</span>
                                    </div>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                        <div style={{ width: '20px', height: '20px', backgroundColor: 'var(--hold)', borderRadius: '4px' }}></div>
                                        <span style={{ fontSize: '0.875rem' }}>선택 중</span>
                                    </div>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                        <div style={{ width: '20px', height: '20px', backgroundColor: 'var(--occupied)', borderRadius: '4px' }}></div>
                                        <span style={{ fontSize: '0.875rem' }}>판매 완료</span>
                                    </div>
                                </div>
                            </div>

                        </div>
                    ) : (
                        <div className="card" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '400px', color: 'var(--text-muted)' }}>
                            좌측 탐색 창에서 영역 또는 진영을 선택해 주세요.
                        </div>
                    )}
                </main>

                {/* Right Sidebar (Selection Summary) */}
                <aside className="sidebar card" style={{ padding: '1.5rem 1rem' }}>
                    <h3 style={{ marginBottom: '1rem', fontSize: '1.125rem' }}>선택 내역</h3>
                    <div style={{
                        marginBottom: '1rem',
                        minHeight: '200px',
                        maxHeight: '400px',
                        overflowY: 'auto',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '0.5rem'
                    }}>
                        {heldSeats.length === 0 ? (
                            <div style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '1rem 0' }}>
                                선택된 좌석이 없습니다.
                            </div>
                        ) : (
                            heldSeats.map((seat) => (
                                <div key={seat.id} style={{
                                    padding: '0.75rem',
                                    backgroundColor: '#f8fafc',
                                    borderRadius: '8px',
                                    border: '1px solid #e2e8f0',
                                    fontSize: '0.875rem'
                                }}>
                                    <div style={{ fontWeight: '600' }}>{seat.blockName}</div>
                                    <div style={{ color: 'var(--text-muted)' }}>{seat.rowNumber}행 {seat.seatNumber}번</div>
                                </div>
                            ))
                        )}
                    </div>

                    <div style={{ borderTop: '1px solid #e2e8f0', paddingTop: '1rem' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem', fontSize: '1rem', fontWeight: 'bold' }}>
                            <span>총 선택</span>
                            <span style={{ color: 'var(--primary-color)' }}>{heldSeats.length}석</span>
                        </div>
                        <button
                            onClick={handleComplete}
                            disabled={heldSeats.length === 0}
                            style={{
                                padding: '0.75rem',
                                backgroundColor: heldSeats.length > 0 ? 'var(--primary-color)' : '#94a3b8',
                                color: 'white',
                                fontSize: '1rem',
                                width: '100%',
                                borderRadius: '8px'
                            }}
                        >
                            선택 완료
                        </button>
                    </div>
                </aside>
            </div>
        </div>
    );
};

export default SeatSelectionPage;
