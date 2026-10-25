// import { useState } from 'react';
import { useEffect, useState } from 'react';
import { ArrowUpRight, FileText, Check, CalendarDays, X, Upload, Mail, MapPin } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { useRecruiting } from './store';
import { CompanyLogo } from './jobs';
import { stageClass, formatDate, type Application, type Stage, type Interview, transitions } from './data';
import { toast } from 'sonner';

type BackendApplication = {
  applicationId: number;
  applicantId: number;
  jobPostingId: number;
  jobTitle: string;
  companyName: string;
  stage: string;
  submittedAt: string;
  resumeFilename: string | null;
};

type BackendApplicationDetail = {
  applicationId: number;
  jobTitle: string;
  companyName: string;
  stage: string;
  stageUpdatedAt: string;
  stageHistory: {
    stage: string;
    changedAt: string;
    changedByUserId: number | null;
  }[];
};

type BackendStage =
    | 'SUBMITTED'
    | 'UNDER_REVIEW'
    | 'INTERVIEW'
    | 'OFFER'
    | 'ACCEPTED'
    | 'DECLINED'
    | 'REJECTED'
    | 'WITHDRAWN';

const displayStage = (stage: string): Stage => {
  const stages: Record<BackendStage, Stage> = {
    SUBMITTED: 'Submitted',
    UNDER_REVIEW: 'Under review',
    INTERVIEW: 'Interview',
    OFFER: 'Offer',
    ACCEPTED: 'Accepted',
    DECLINED: 'Declined',
    REJECTED: 'Rejected',
    WITHDRAWN: 'Withdrawn',
  };

  return stages[stage as BackendStage] ?? 'Submitted';
};

export function ApplicationsPage({ candidates = false }: { candidates?: boolean }) {
  const { applications, jobs, profile, role, updateStage } = useRecruiting();
  const [backendApplications, setBackendApplications] =
      useState<BackendApplication[]>([]);
  const [backendDetail, setBackendDetail] =
      useState<BackendApplicationDetail | null>(null);

  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState('');

  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState('');

  const applicantId = 1; // Temporary test applicant ID

  const [selectedId, setSelectedId] = useState<number | null>(null);

  useEffect(() => {
    if (candidates || role !== 'Applicant') return;

    const loadApplications = async () => {
      setLoading(true);
      setLoadError('');

      try {
        const response = await fetch(
            `/api/applications?applicantId=${applicantId}`
        );

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }

        const data: BackendApplication[] = await response.json();
        setBackendApplications(data);
      } catch (error) {
        setLoadError('Failed to load applications.');
        console.error(error);
      } finally {
        setLoading(false);
      }
    };

    void loadApplications();
  }, [candidates, role]);

  useEffect(() => {
    if (candidates || role !== 'Applicant' || selectedId === null) {
      setBackendDetail(null);
      return;
    }

    const controller = new AbortController();

    const loadDetail = async () => {
      setBackendDetail(null);
      setDetailLoading(true);
      setDetailError('');

      try {
        const response = await fetch(
            `/api/applications/${selectedId}`,
            {
              headers: {
                'X-Applicant-Id': String(applicantId),
              },
              signal: controller.signal,
            }
        );

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }

        const data: BackendApplicationDetail = await response.json();

        if (!controller.signal.aborted) {
          setBackendDetail(data);
        }
      } catch (error) {
        if (!controller.signal.aborted) {
          console.error(error);
          setDetailError('Failed to load application history.');
        }
      } finally {
        if (!controller.signal.aborted) {
          setDetailLoading(false);
        }
      }
    };

    void loadDetail();

    return () => controller.abort();
  }, [selectedId, candidates, role, applicantId]);
  const [tab, setTab] = useState('All');
  // const [selectedId, setSelectedId] = useState<number | null>(null);
  const [confirm, setConfirm] = useState<Stage | null>(null);
  const [scheduleApp, setScheduleApp] = useState<Application | null>(null);
  const [search, setSearch] = useState('');
  const owned = applications.filter(a => candidates ? role === 'Recruiter' && jobs.find(j => j.id === a.jobId)?.company === 'Linear' : role === 'Applicant' && a.name === profile.name);
  const filtered = owned.filter(a => (tab === 'All' || (tab === 'Active' ? ['Submitted', 'Under review', 'Interview'].includes(a.stage) : tab === 'Offers' ? a.stage === 'Offer' : ['Accepted', 'Declined', 'Rejected', 'Withdrawn'].includes(a.stage))) && `${a.name} ${jobs.find(j => j.id === a.jobId)?.title}`.toLowerCase().includes(search.toLowerCase()));
  const selected = applications.find(a => a.id === selectedId);
  const job = jobs.find(j => j.id === selected?.jobId);


  if (!candidates && role === 'Applicant') {
    const filteredBackend = backendApplications.filter(app => {
      const stage = displayStage(app.stage);

      const matchesTab =
          tab === 'All' ||
          (tab === 'Active' &&
              ['Submitted', 'Under review', 'Interview'].includes(stage)) ||
          (tab === 'Offers' && stage === 'Offer') ||
          (tab === 'Closed' &&
              ['Accepted', 'Declined', 'Rejected', 'Withdrawn'].includes(stage));

      const matchesSearch =
          `${app.jobTitle} ${app.companyName}`
              .toLowerCase()
              .includes(search.toLowerCase());

      return matchesTab && matchesSearch;
    });

    return (
        <>
          <div className="mb-7">
            <p className="mb-2 text-[10px] font-semibold uppercase tracking-[1.4px] text-primary">
              YOUR CAREER IN MOTION
            </p>

            <h1 className="page-heading">My applications</h1>

            <p className="mt-2 text-sm text-muted-foreground">
              Every step forward, together in one place.
            </p>
          </div>

          <div className="mb-8 grid grid-cols-2 gap-5 border-y py-6 sm:grid-cols-4">
            {[
              { title: 'Total applications', count: backendApplications.length },
              {
                title: 'In progress',
                count: backendApplications.filter(a =>
                    ['SUBMITTED', 'UNDER_REVIEW', 'INTERVIEW'].includes(a.stage)
                ).length,
              },
              {
                title: 'Interviews',
                count: backendApplications.filter(a => a.stage === 'INTERVIEW').length,
              },
              {
                title: 'Offers',
                count: backendApplications.filter(a => a.stage === 'OFFER').length,
              },
            ].map((item, index) => (
                <div key={item.title} className={index ? 'sm:border-l sm:pl-6' : ''}>
                  <p className="text-xs text-muted-foreground">{item.title}</p>
                  <p className="mt-2 font-display text-3xl font-semibold">
                    {String(item.count).padStart(2, '0')}
                  </p>
                </div>
            ))}
          </div>

          <div className="mb-5 flex flex-wrap items-center justify-between gap-4">
            <div className="flex gap-1">
              {['All', 'Active', 'Offers', 'Closed'].map(t => (
                  <Button
                      key={t}
                      size="sm"
                      variant={tab === t ? 'secondary' : 'ghost'}
                      onClick={() => setTab(t)}
                  >
                    {t}
                  </Button>
              ))}
            </div>

            <input
                aria-label="Search applications"
                className="form-input max-w-[240px]"
                placeholder="Search applications..."
                value={search}
                onChange={e => setSearch(e.target.value)}
            />
          </div>

          {loading && <p className="py-6 text-sm">Loading applications...</p>}

          {loadError && (
              <p className="py-6 text-sm text-destructive">{loadError}</p>
          )}

          {!loading && !loadError && (
              <div className="overflow-x-auto rounded-md border bg-card">
                <table className="data-table">
                  <thead>
                  <tr>
                    <th>Opportunity</th>
                    <th>Submitted</th>
                    <th>Stage</th>
                    <th></th>
                  </tr>
                  </thead>

                  <tbody>
                  {filteredBackend.map(app => (
                      <tr key={app.applicationId}>
                        <td>
                          <p className="font-medium">{app.jobTitle}</p>
                          <p className="mt-1 text-[11px] text-muted-foreground">
                            {app.companyName}
                          </p>
                        </td>

                        <td className="text-xs text-muted-foreground">
                          {new Date(app.submittedAt).toLocaleDateString('en-US', {
                            month: 'short',
                            day: 'numeric',
                            year: 'numeric',
                          })}
                        </td>

                        <td>
                    <span className={`status ${stageClass(displayStage(app.stage))}`}>
                      {displayStage(app.stage)}
                    </span>
                        </td>

                        <td>
                          <Button
                              variant="ghost"
                              size="icon"
                              title="View application"
                              onClick={() => setSelectedId(app.applicationId)}
                          >
                            <ArrowUpRight />
                          </Button>
                        </td>
                      </tr>
                  ))}
                  </tbody>
                </table>

                {filteredBackend.length === 0 && (
                    <div className="empty-state">
                      <FileText className="mx-auto mb-3 size-7" />
                      <p>No applications in this view.</p>
                    </div>
                )}
              </div>
          )}

          <Dialog
              open={selectedId !== null}
              onOpenChange={open => {
                if (!open) {
                  setSelectedId(null);
                }
              }}
          >
            <DialogContent>
              {(() => {
                const app = backendApplications.find(
                    a => a.applicationId === selectedId
                );

                if (!app) return null;

                return (
                    <>
                      <DialogTitle>{app.jobTitle}</DialogTitle>

                      <DialogDescription>
                        {app.companyName}
                      </DialogDescription>

                      <span className={`status w-fit ${stageClass(displayStage(app.stage))}`}>
                  {displayStage(app.stage)}
                </span>

                      <section className="border-t pt-4">
                        <h3 className="text-sm font-semibold">
                          Application information
                        </h3>

                        <p className="mt-3 text-sm">
                          Application ID: {app.applicationId}
                        </p>

                        <p className="mt-2 text-sm">
                          Resume: {app.resumeFilename ?? 'Not available'}
                        </p>
                      </section>

                      <section className="border-t pt-4">
                        <h3 className="text-sm font-semibold">
                          Application timeline
                        </h3>

                        {detailLoading && (
                            <p className="mt-3 text-sm text-muted-foreground">
                              Loading timeline...
                            </p>
                        )}

                        {detailError && (
                            <p className="mt-3 text-sm text-destructive">
                              {detailError}
                            </p>
                        )}

                        {backendDetail && (
                            <ol className="mt-4 space-y-5 border-l pl-5">
                              {backendDetail.stageHistory.map((event, index) => (
                                  <li key={index} className="relative">
                                    <span className="absolute -left-[25px] top-1 size-2 rounded-full bg-primary" />

                                    <p className="text-xs font-semibold">
                                      {displayStage(event.stage)}
                                    </p>

                                    <p className="mt-1 text-[11px] text-muted-foreground">
                                      {new Date(event.changedAt).toLocaleString('en-US')}
                                    </p>
                                  </li>
                              ))}
                            </ol>
                        )}
                      </section>

                    </>
                );
              })()}
            </DialogContent>
          </Dialog>
        </>
    );
  }


  // // if (!candidates && role === 'Applicant') {
  //   return (
  //       <div className="p-6">
  //         <h1 className="page-heading">My applications</h1>
  //
  //         {loading && <p>Loading applications...</p>}
  //
  //         {loadError && (
  //             <p className="text-red-500">{loadError}</p>
  //         )}
  //
  //         {!loading && !loadError && (
  //             <div className="mt-6 space-y-4">
  //               {backendApplications.map(app => (
  //                   <div
  //                       key={app.applicationId}
  //                       className="rounded-md border bg-card p-5"
  //                   >
  //                     <h2 className="font-semibold">
  //                       {app.jobTitle}
  //                     </h2>
  //
  //                     <p>{app.companyName}</p>
  //                     <p>Application ID: {app.applicationId}</p>
  //                     <p>Stage: {app.stage}</p>
  //                   </div>
  //               ))}
  //             </div>
  //         )}
  //       </div>
  //   );
  // }

  return <><div className="mb-7"><p className="mb-2 text-[10px] font-semibold uppercase tracking-[1.4px] text-primary">{candidates ? 'FIND YOUR NEXT TEAMMATE' : 'YOUR CAREER IN MOTION'}</p><h1 className="page-heading">{candidates ? 'Candidate pipeline' : 'My applications'}</h1><p className="mt-2 text-sm text-muted-foreground">{candidates ? 'Thoughtful hiring starts with a clear view of every candidate.' : 'Every step forward, together in one place.'}</p></div><div className="mb-8 grid grid-cols-2 gap-5 border-y py-6 sm:grid-cols-4">{[{ title: 'Total applications', count: owned.length }, { title: 'In progress', count: owned.filter(a => ['Submitted', 'Under review', 'Interview'].includes(a.stage)).length }, { title: 'Interviews', count: owned.filter(a => a.stage === 'Interview').length }, { title: 'Offers', count: owned.filter(a => a.stage === 'Offer').length }].map((s, i) => <div key={s.title} className={i ? 'sm:border-l sm:pl-6' : ''}><p className="text-xs text-muted-foreground">{s.title}</p><p className={`mt-2 font-display text-3xl font-semibold ${i === 3 ? 'text-status-amber' : ''}`}>{s.count.toString().padStart(2, '0')}</p></div>)}</div><div className="mb-5 flex flex-wrap items-center justify-between gap-4"><div className="flex gap-1">{['All', 'Active', 'Offers', 'Closed'].map(t => <Button key={t} size="sm" variant={tab === t ? 'secondary' : 'ghost'} onClick={() => setTab(t)}>{t}</Button>)}</div><input aria-label="Search applications" className="form-input max-w-[240px]" placeholder={candidates ? 'Search candidates or roles...' : 'Search applications...'} value={search} onChange={e => setSearch(e.target.value)} /></div><div className="overflow-x-auto rounded-md border bg-card"><table className="data-table"><thead><tr><th>{candidates ? 'Candidate' : 'Opportunity'}</th>{candidates && <th>Role</th>}<th>Submitted</th><th>Stage</th><th></th></tr></thead><tbody>{filtered.map(a => { const j = jobs.find(j => j.id === a.jobId); if (!j) return null; return <tr key={a.id}><td><div className="flex items-center gap-3">{candidates ? <span className="avatar">{a.name.split(' ').map(n => n[0]).join('')}</span> : <CompanyLogo job={j} />}<div><p className="whitespace-nowrap font-medium">{candidates ? a.name : j.title}</p><p className="mt-1 text-[11px] text-muted-foreground">{candidates ? a.email : j.company}</p></div></div></td>{candidates && <td>{j.title}</td>}<td className="whitespace-nowrap text-xs text-muted-foreground">{a.date}</td><td><span className={`status ${stageClass(a.stage)}`}>{a.stage}</span></td><td><Button variant="ghost" size="icon" title="View application" aria-label={`View application ${a.name} ${j.title}`} onClick={() => setSelectedId(a.id)}><ArrowUpRight /></Button></td></tr>; })}</tbody></table>{!filtered.length && <div className="empty-state"><FileText className="mx-auto mb-3 size-7" /><p>No applications in this view.</p></div>}</div>
    <Dialog open={Boolean(selected)} onOpenChange={o => { if (!o) { setSelectedId(null); setConfirm(null); } }}><DialogContent className="max-h-[90vh] max-w-[620px] overflow-y-auto">{selected && job && <><DialogTitle>{candidates ? selected.name : job.title}</DialogTitle><DialogDescription>{candidates ? job.title : job.company} · Applied {selected.date}</DialogDescription><span className={`status w-fit ${stageClass(selected.stage)}`}>{selected.stage}</span><section className="border-t pt-4"><h3 className="text-sm font-semibold">{candidates ? 'Applicant information' : 'Application materials'}</h3><p className="mt-3 text-sm">{selected.name}</p><p className="mt-1 text-xs text-muted-foreground">{selected.email}</p><p className="mt-4 flex items-center gap-2 text-xs"><FileText className="size-4 text-primary" />{selected.resume}</p>{candidates && <p className="mt-3 text-xs leading-6 text-muted-foreground">Resume preview is represented by sample file information in this frontend prototype.</p>}</section><section className="border-t pt-4"><h3 className="text-sm font-semibold">Application timeline</h3><ol className="mt-4 space-y-5 border-l pl-5">{selected.history.map((h, i) => <li key={i} className="relative"><span className="absolute -left-[25px] top-1 size-2 rounded-full bg-primary" /><p className="text-xs font-semibold">{h.stage}</p><p className="mt-1 text-[11px] text-muted-foreground">{h.date} · {h.actor}</p></li>)}</ol></section>{confirm ? <section className="border-t pt-4"><p className="text-sm">{confirm === 'Withdrawn' ? 'Withdraw this application? You will not be able to reapply to this posting.' : `Confirm changing the application to ${confirm.toLowerCase()}?`}</p><div className="mt-4 flex justify-end gap-2"><Button variant="outline" onClick={() => setConfirm(null)}>Cancel</Button><Button variant={['Withdrawn', 'Rejected', 'Declined'].includes(confirm) ? 'destructive' : 'default'} onClick={() => { updateStage(selected, confirm); setConfirm(null); }}>Confirm {confirm.toLowerCase()}</Button></div></section> : <div className="flex flex-wrap gap-2 border-t pt-5">{candidates ? <>{(transitions[selected.stage] ?? []).map(s => <Button key={s} variant={s === 'Rejected' ? 'outline' : 'default'} onClick={() => setConfirm(s)}>{s === 'Under review' ? 'Start review' : s === 'Offer' ? 'Make offer' : 'Reject'}</Button>)}{selected.stage === 'Under review' && <Button variant="outline" onClick={() => setScheduleApp(selected)}><CalendarDays />Schedule interview</Button>}</> : <>{['Submitted', 'Under review', 'Interview'].includes(selected.stage) && <Button variant="outline" onClick={() => setConfirm('Withdrawn')}>Withdraw application</Button>}{selected.stage === 'Offer' && <><Button onClick={() => setConfirm('Accepted')}><Check />Accept offer</Button><Button variant="outline" onClick={() => setConfirm('Declined')}>Decline offer</Button></>}</>}</div>}</>}</DialogContent></Dialog><InterviewForm application={scheduleApp} onClose={() => setScheduleApp(null)} />
  </>;
}

export function InterviewForm({ application, existing, onClose }: { application: Application | null; existing?: Interview | undefined; onClose: () => void }) {
  const { schedule } = useRecruiting();
  return <Dialog open={Boolean(application)} onOpenChange={o => { if (!o) onClose(); }}><DialogContent><DialogTitle>{existing ? 'Edit interview' : 'Schedule interview'}</DialogTitle><DialogDescription>Arrange an interview with {application?.name}.</DialogDescription><form key={existing?.id ?? application?.id} className="space-y-4" onSubmit={e => { e.preventDefault(); if (!application) return; const f = new FormData(e.currentTarget); const date = String(f.get('date')); const time = String(f.get('time')); if (new Date(`${date}T${time}`) < new Date()) { toast.error('Choose a future date and time.'); return; } schedule({ id: existing?.id ?? Date.now(), applicationId: application.id, date, time, location: String(f.get('location')), notes: String(f.get('notes')), status: 'Scheduled' }); onClose(); }}><div className="grid grid-cols-2 gap-3"><label className="field">Date<input type="date" name="date" required min="2026-10-15" defaultValue={existing?.date} /></label><label className="field">Time<input type="time" name="time" required defaultValue={existing?.time ?? '10:00'} /></label></div><label className="field">Location or meeting link<input name="location" required defaultValue={existing?.location} placeholder="Office address or meeting link" /></label><label className="field">Interview notes<textarea name="notes" defaultValue={existing?.notes} placeholder="Topics, interviewers, or preparation materials" /></label><div className="flex justify-end gap-2"><Button variant="outline" type="button" onClick={onClose}>Cancel</Button><Button type="submit">Save interview</Button></div></form></DialogContent></Dialog>;
}
export function InterviewsPage() {
  const { interviews, applications, jobs, profile, role, setInterviews, record } = useRecruiting();
  const [edit, setEdit] = useState<Interview | null>(null);
  const [cancel, setCancel] = useState<Interview | null>(null);
  const [tab, setTab] = useState('Upcoming');
  const visible = interviews.filter(i => { const a = applications.find(a => a.id === i.applicationId); const j = jobs.find(j => j.id === a?.jobId); return (role === 'Applicant' ? a?.name === profile.name : role === 'Recruiter' && j?.company === 'Linear') && (tab === 'Cancelled' ? i.status === 'Cancelled' : i.status === 'Scheduled'); });
  return <><h1 className="page-heading">Interviews</h1><p className="mt-2 text-sm text-muted-foreground">Good conversations open new doors.</p><div className="my-6 flex gap-1">{['Upcoming', 'Cancelled'].map(t => <Button key={t} size="sm" variant={t === tab ? 'secondary' : 'ghost'} onClick={() => setTab(t)}>{t}</Button>)}</div><div className="space-y-4">{visible.map(i => { const a = applications.find(a => a.id === i.applicationId); const j = jobs.find(j => j.id === a?.jobId); if (!a || !j) return null; return <article key={i.id} className="flex flex-wrap items-start gap-5 rounded-md border bg-card p-6"><div className="w-16 shrink-0 rounded-md bg-secondary py-3 text-center text-primary"><p className="text-[10px] font-medium uppercase">{new Date(i.date + 'T12:00:00').toLocaleDateString('en-US', { month: 'short' })}</p><p className="mt-1 font-display text-3xl font-semibold">{i.date.slice(-2)}</p></div><div className="min-w-0 flex-1"><h2 className="section-heading">{j.title}</h2><p className="mt-1 text-xs text-muted-foreground">{role === 'Recruiter' ? a.name : j.company} · {formatDate(i.date)} at {i.time}</p><p className="mt-4 flex items-start gap-2 text-xs"><MapPin className="size-3.5 shrink-0" />{i.location}</p><p className="mt-2 text-xs leading-6 text-muted-foreground">{i.notes}</p></div><div className="flex flex-col items-end gap-3"><span className={`status ${i.status === 'Scheduled' ? 'interview' : ''}`}>{i.status}</span>{role === 'Recruiter' && i.status === 'Scheduled' && <div className="flex gap-2"><Button variant="outline" size="sm" onClick={() => setEdit(i)}>Edit</Button><Button variant="ghost" size="sm" onClick={() => setCancel(i)}>Cancel</Button></div>}</div></article>; })}{!visible.length && <div className="empty-state"><CalendarDays className="mx-auto mb-4 size-8" /><p>No {tab.toLowerCase()} interviews.</p></div>}</div><InterviewForm application={applications.find(a => a.id === edit?.applicationId) ?? null} existing={edit ?? undefined} onClose={() => setEdit(null)} /><Dialog open={Boolean(cancel)} onOpenChange={o => { if (!o) setCancel(null); }}><DialogContent><DialogTitle>Cancel interview?</DialogTitle><DialogDescription>The candidate’s application will remain in the interview stage.</DialogDescription><div className="flex justify-end gap-2"><Button variant="outline" onClick={() => setCancel(null)}>Keep interview</Button><Button variant="destructive" onClick={() => { setInterviews(prev => prev.map(i => i.id === cancel?.id ? { ...i, status: 'Cancelled' } : i)); record('Cancelled interview'); toast.success('Interview cancelled.'); setCancel(null); }}>Cancel interview</Button></div></DialogContent></Dialog></>;
}
export function ProfilePage() {
  const { profile, setProfile, role, record } = useRecruiting();
  const [draft, setDraft] = useState(profile);
  if (role !== 'Applicant') return <div className="empty-state">Choose the applicant workspace to view a profile.</div>;
  return <><h1 className="page-heading">My profile</h1><p className="mt-2 text-sm text-muted-foreground">A little about you. A lot of possibility.</p><div className="my-8 flex items-center gap-5 border-y py-6"><span className="avatar !size-16 !text-xl">{profile.name.split(' ').map(n => n[0]).join('')}</span><div><h2 className="section-heading">{profile.name}</h2><p className="mt-1 text-xs text-muted-foreground">{profile.headline}</p><p className="mt-2 flex items-center gap-1 text-[11px] text-primary"><MapPin className="size-3" />{profile.location}</p></div></div><form className="max-w-[760px]" onSubmit={e => { e.preventDefault(); setProfile(draft); record('Updated applicant profile'); toast.success('Profile saved.'); }}><h3 className="section-heading">Personal information</h3><div className="mt-5 grid grid-cols-1 gap-5 sm:grid-cols-2">{[{ key: 'name', label: 'Full name', type: 'text' }, { key: 'email', label: 'Email address', type: 'email' }, { key: 'phone', label: 'Phone number', type: 'tel' }, { key: 'location', label: 'Location', type: 'text' }].map(f => <label key={f.key} className="field">{f.label}<input required={['name', 'email'].includes(f.key)} type={f.type} value={draft[f.key as keyof typeof draft]} onChange={e => setDraft({ ...draft, [f.key]: e.target.value })} /></label>)}</div><label className="field mt-5">Professional headline<input value={draft.headline} onChange={e => setDraft({ ...draft, headline: e.target.value })} /></label><label className="field mt-5">About you<textarea value={draft.bio} onChange={e => setDraft({ ...draft, bio: e.target.value })} /></label><h3 className="section-heading mb-4 mt-8 border-t pt-6">Resume & materials</h3><div className="flex flex-wrap items-center justify-between gap-3 rounded-md border bg-card p-4"><div className="flex items-center gap-3"><FileText className="size-5 text-primary" /><div><p className="text-xs font-medium">{draft.resume || 'No resume uploaded'}</p><p className="mt-1 text-[10px] text-muted-foreground">PDF or DOCX · Maximum 10 MB</p></div></div><div className="flex items-center gap-2"><Button variant="outline" size="sm" asChild><label className="cursor-pointer"><Upload />Upload<input aria-label="Upload profile resume" className="sr-only" type="file" accept=".pdf,.docx" onChange={e => { const f = e.target.files?.[0]; if (!f) return; if (!/\.(pdf|docx)$/i.test(f.name) || f.size > 10 * 1024 * 1024) { toast.error('Choose a PDF or DOCX file under 10 MB.'); return; } setDraft({ ...draft, resume: f.name }); }} /></label></Button>{draft.resume && <Button variant="ghost" size="icon" title="Remove resume" aria-label="Remove resume" onClick={() => setDraft({ ...draft, resume: '' })}><X /></Button>}</div></div><div className="mt-7 flex justify-end gap-3 border-t pt-5"><Button type="button" variant="outline" onClick={() => setDraft(profile)}>Discard changes</Button><Button type="submit">Save profile <Check /></Button></div></form></>;
}