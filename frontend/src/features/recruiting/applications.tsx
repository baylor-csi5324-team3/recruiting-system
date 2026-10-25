import { useEffect, useState, type FormEvent } from 'react';
import { ArrowUpRight, FileText, CalendarDays, MapPin, Upload } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { useRecruiting } from './store';
import { CompanyLogo } from './jobs';
import { stageClass, formatDate, type Application, type Stage, type Interview } from './data';
import { type UserProfile } from '@/lib/recruiting-api';
import { toast } from 'sonner';

const activeStages: Stage[]=['Submitted','Under review','Interview'];
const closedStages: Stage[]=['Accepted','Declined','Rejected','Withdrawn'];
export function ApplicationsPage({candidates=false}:{candidates?:boolean}) {
  const {applications,jobs,role,actor,updateStage,loading,error,refresh}=useRecruiting();
  const [tab,setTab]=useState('All');
  const [search,setSearch]=useState('');
  const [selectedId,setSelectedId]=useState<number|null>(null);
  const [confirm,setConfirm]=useState<Stage|null>(null);
  const [scheduleApp,setScheduleApp]=useState<Application|null>(null);
  const [busy,setBusy]=useState(false);
  const selected=applications.find(a=>a.id===selectedId);
  const selectedJob=jobs.find(j=>j.id===selected?.jobId);
  const visible=applications.filter(a=>
    (tab==='All'||(tab==='Active'?activeStages.includes(a.stage):tab==='Offers'?a.stage==='Offer':closedStages.includes(a.stage))) &&
    `${a.name} ${a.email} ${jobs.find(j=>j.id===a.jobId)?.title||''}`.toLowerCase().includes(search.toLowerCase()));
  const downloadResume = async (application: Application) => {
    if (role !== 'Recruiter' || !actor) {
      toast.error('Only recruiters may download submitted resumes.');
      return;
    }
    setBusy(true);
    try {
      const response = await fetch(`/api/applications/${application.id}/resume`, {
        headers: { 'X-Recruiter-Id': String(actor.id) },
      });
      if (!response.ok) {
        throw new Error(response.status === 404
          ? 'No submitted resume file is available for this application.'
          : `Resume download failed (HTTP ${response.status})`);
      }
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = application.resume || 'resume';
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Unable to download resume.');
    } finally {
      setBusy(false);
    }
  };
  const change=async()=>{
    if(!selected||!confirm)return;
    setBusy(true); const success=await updateStage(selected,confirm); setBusy(false);
    if(success){setSelectedId(null);setConfirm(null);}
  };
  return (
    <>
      <div className="mb-7">
        <p className="mb-2 text-[10px] font-semibold uppercase tracking-[1.4px] text-primary">
          {candidates ? "FIND YOUR NEXT TEAMMATE" : "YOUR CAREER IN MOTION"}
        </p>
        <h1 className="page-heading">{candidates ? "Candidate pipeline" : "My applications"}</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          All applications are loaded from Spring Boot.
        </p>
      </div>
      <div className="mb-8 grid grid-cols-2 gap-5 border-y py-6 sm:grid-cols-4">
        {[
          ["Total applications", applications.length],
          ["In progress", applications.filter((a) => activeStages.includes(a.stage)).length],
          ["Interviews", applications.filter((a) => a.stage === "Interview").length],
          ["Offers", applications.filter((a) => a.stage === "Offer").length],
        ].map(([title, n], i) => (
          <div key={title} className={i ? "sm:border-l sm:pl-6" : ""}>
            <p className="text-xs text-muted-foreground">{title}</p>
            <p className="mt-2 font-display text-3xl font-semibold">{String(n).padStart(2, "0")}</p>
          </div>
        ))}
      </div>
      <div className="mb-5 flex flex-wrap items-center justify-between gap-4">
        <div className="flex gap-1">
          {["All", "Active", "Offers", "Closed"].map((t) => (
            <Button
              size="sm"
              variant={tab === t ? "secondary" : "ghost"}
              key={t}
              onClick={() => setTab(t)}
            >
              {t}
            </Button>
          ))}
        </div>
        <div className="flex gap-2">
          <input
            className="form-input max-w-[240px]"
            aria-label="Search applications"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search candidates or jobs"
          />
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              void refresh();
            }}
          >
            Refresh
          </Button>
        </div>
      </div>
      {loading ? (
        <p className="text-sm">Loading applications...</p>
      ) : error ? (
        <p className="text-sm text-destructive">{error}</p>
      ) : (
        <div className="overflow-x-auto rounded-md border bg-card">
          <table className="data-table">
            <thead>
              <tr>
                <th>{candidates ? "Candidate" : "Opportunity"}</th>
                {candidates && <th>Role</th>}
                <th>Submitted</th>
                <th>Stage</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {visible.map((a) => {
                const job = jobs.find((j) => j.id === a.jobId);
                return (
                  <tr key={a.id}>
                    <td>
                      <div className="flex items-center gap-3">
                        {job && <CompanyLogo job={job} />}
                        <div>
                          <p className="font-medium">
                            {candidates ? a.name : (job?.title ?? "Job unavailable")}
                          </p>
                          <p className="text-[11px] text-muted-foreground">
                            {candidates ? a.email : job?.company}
                          </p>
                        </div>
                      </div>
                    </td>
                    {candidates && <td>{job?.title}</td>}
                    <td className="text-xs">{a.date}</td>
                    <td>
                      <span className={`status ${stageClass(a.stage)}`}>{a.stage}</span>
                    </td>
                    <td>
                      <Button
                        variant="ghost"
                        size="icon"
                        aria-label="View application"
                        onClick={() => setSelectedId(a.id)}
                      >
                        <ArrowUpRight />
                      </Button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          {!visible.length && (
            <div className="empty-state">
              <FileText className="mx-auto mb-3 size-7" />
              No applications in this view.
            </div>
          )}
        </div>
      )}
      <Dialog
        open={selectedId !== null}
        onOpenChange={(o) => {
          if (!o) {
            setSelectedId(null);
            setConfirm(null);
          }
        }}
      >
        <DialogContent className="max-h-[90vh] max-w-[620px] overflow-y-auto">
          {selected && (
            <>
              <DialogTitle>
                {candidates ? selected.name : (selectedJob?.title ?? "Application")}
              </DialogTitle>
              <DialogDescription>
                {selectedJob?.title ?? "Job"} · Applied {selected.date}
              </DialogDescription>
              <span className={`status w-fit ${stageClass(selected.stage)}`}>{selected.stage}</span>
              <section className="border-t pt-4">
                <h3 className="text-sm font-semibold">Application information</h3>
                <p className="mt-3 text-sm">{selected.name}</p>
                <p className="mt-1 text-xs text-muted-foreground">{selected.email}</p>
                <div className="mt-4 flex items-center gap-2 text-xs">
                  <FileText className="size-4" />
                  <span>{selected.resume || "No resume filename recorded"}</span>
                  {role === "Recruiter" && selected.resume && (
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      disabled={busy}
                      onClick={() => { void downloadResume(selected); }}
                    >
                      Download resume
                    </Button>
                  )}
                </div>
              </section>
              <section className="border-t pt-4">
                <h3 className="text-sm font-semibold">Stage history</h3>
                <ol className="mt-4 space-y-3 border-l pl-5">
                  {selected.history.map((h, i) => (
                    <li key={i} className="text-xs">
                      {h.stage} · {h.date} · {h.actor}
                    </li>
                  ))}
                </ol>
              </section>
              {confirm ? (
                <div className="border-t pt-4">
                  <p className="text-sm">Confirm changing this application to {confirm}?</p>
                  <div className="mt-4 flex justify-end gap-2">
                    <Button variant="outline" onClick={() => setConfirm(null)}>
                      Cancel
                    </Button>
                    <Button
                      disabled={busy}
                      onClick={() => {
                        void change();
                      }}
                    >
                      Confirm
                    </Button>
                  </div>
                </div>
              ) : (
                <div className="flex flex-wrap justify-end gap-2 border-t pt-4">
                  {role === "Applicant" &&
                    ["Submitted", "Under review", "Interview"].includes(selected.stage) && (
                      <Button variant="outline" onClick={() => setConfirm("Withdrawn")}>
                        Withdraw
                      </Button>
                    )}
                  {role === "Applicant" && selected.stage === "Offer" && (
                    <p className="text-xs text-muted-foreground">
                      Please respond to the offer via email.
                    </p>
                  )}
                  {role === "Recruiter" && (
                    <>
                      {selected.stage === "Submitted" && (
                        <Button onClick={() => setConfirm("Under review")}>Start review</Button>
                      )}
                      {selected.stage === "Under review" && (
                        <Button onClick={() => setConfirm("Interview")}>Move to interview</Button>
                      )}
                      {["Submitted", "Under review", "Interview"].includes(selected.stage) && (
                        <Button variant="outline" onClick={() => setConfirm("Rejected")}>
                          Reject
                        </Button>
                      )}
                      {["Under review", "Interview"].includes(selected.stage) && (
                        <Button onClick={() => setConfirm("Offer")}>Extend offer</Button>
                      )}
                      {role === "Recruiter" && selected.stage === "Offer" && (
                        <>
                          <Button variant="outline" onClick={() => setConfirm("Declined")}>
                            Record declined
                          </Button>

                          <Button onClick={() => setConfirm("Accepted")}>Record accepted</Button>
                        </>
                      )}
                      {selected.stage === "Interview" && (
                        <Button variant="outline" onClick={() => setScheduleApp(selected)}>
                          Schedule interview
                        </Button>
                      )}
                    </>
                  )}
                </div>
              )}
            </>
          )}
        </DialogContent>
      </Dialog>
      <InterviewForm application={scheduleApp} onClose={() => setScheduleApp(null)} />
    </>
  );
}

export function InterviewForm({application,existing,onClose}:{application:Application|null;existing?:Interview;onClose:()=>void}) {
  const {schedule}=useRecruiting();
  const [saving,setSaving]=useState(false);
  const handle=async(e:FormEvent<HTMLFormElement>)=>{
    e.preventDefault();if(!application)return;
    const f=new FormData(e.currentTarget);const date=String(f.get('date')),time=String(f.get('time'));
    if(new Date(`${date}T${time}`)<new Date()){toast.error('Choose a future date and time.');return;}
    setSaving(true);
    const ok=await schedule({id:existing?.id??0,applicationId:application.id,date,time,location:String(f.get('location')),notes:String(f.get('notes')),status:'Scheduled'});
    setSaving(false);if(ok)onClose();
  };
  return <Dialog open={Boolean(application)} onOpenChange={o=>{if(!o)onClose();}}><DialogContent><DialogTitle>{existing?'Edit':'Schedule'} interview</DialogTitle><DialogDescription>Arrange an interview with {application?.name}</DialogDescription><form key={existing?.id??application?.id} className="space-y-4" onSubmit={e=>{void handle(e);}}><div className="grid grid-cols-2 gap-3"><label className="field">Date<input type="date" name="date" required min={new Date().toISOString().slice(0,10)} defaultValue={existing?.date}/></label><label className="field">Time<input type="time" name="time" required defaultValue={existing?.time??'10:00'}/></label></div><label className="field">Location / meeting link<input name="location" required defaultValue={existing?.location}/></label><label className="field">Notes<textarea name="notes" defaultValue={existing?.notes}/></label><div className="flex justify-end gap-2"><Button type="button" variant="outline" onClick={onClose}>Cancel</Button><Button type="submit" disabled={saving}>Save interview</Button></div></form></DialogContent></Dialog>;
}
export function InterviewsPage() {
  const {interviews,applications,jobs,role,schedule,loading}=useRecruiting();
  const [tab,setTab]=useState('Upcoming');const [edit,setEdit]=useState<Interview|null>(null);
  const visible=interviews.filter(i=>i.status===(tab==='Cancelled'?'Cancelled':'Scheduled'));
  return <><h1 className="page-heading">Interviews</h1><p className="mt-2 text-sm text-muted-foreground">Interview schedules are stored on the server.</p><div className="my-6 flex gap-1">{['Upcoming','Cancelled'].map(t=><Button key={t} variant={t===tab?'secondary':'ghost'} size="sm" onClick={()=>setTab(t)}>{t}</Button>)}</div>{loading?<p>Loading...</p>:<div className="space-y-4">{visible.map(i=>{const app=applications.find(a=>a.id===i.applicationId),job=jobs.find(j=>j.id===app?.jobId);return <article key={i.id} className="flex flex-wrap items-start gap-5 rounded-md border bg-card p-6"><div className="min-w-0 flex-1"><h2 className="section-heading">{job?.title??`Application #${i.applicationId}`}</h2><p className="mt-1 text-xs">{role==='Recruiter'?app?.name:job?.company} · {formatDate(i.date)} at {i.time}</p><p className="mt-3 flex gap-2 text-xs"><MapPin className="size-4"/>{i.location}</p><p className="mt-2 text-xs text-muted-foreground">{i.notes}</p></div><div className="flex flex-col gap-2"><span className="status interview">{i.status}</span>{role==='Recruiter'&&i.status==='Scheduled'&&<><Button variant="outline" size="sm" onClick={()=>setEdit(i)}>Edit</Button><Button variant="ghost" size="sm" onClick={()=>{void schedule({...i,status:'Cancelled'});}}>Cancel</Button></>}</div></article>})}{!visible.length&&<div className="empty-state"><CalendarDays className="mx-auto mb-4 size-8"/>No {tab.toLowerCase()} interviews.</div>}</div>}<InterviewForm application={applications.find(a=>a.id===edit?.applicationId)??null} existing={edit??undefined} onClose={()=>setEdit(null)}/></>;
}
export function ProfilePage() {
  const {profile,role,saveProfile,uploadProfileResume,loading}=useRecruiting();
  const [draft,setDraft]=useState<UserProfile>(profile);
  const [saving,setSaving]=useState(false);
  useEffect(()=>{setDraft(profile);},[profile]);
  if(role!=='Applicant')return <div className="empty-state">Choose the applicant workspace to view the profile.</div>;
  const update=(k:keyof UserProfile,v:string)=>setDraft(d=>({...d,[k]:v}));
  const save=async(e:FormEvent<HTMLFormElement>)=>{e.preventDefault();setSaving(true);await saveProfile(draft);setSaving(false);};
  return <><h1 className="page-heading">My profile</h1><p className="mt-2 text-sm text-muted-foreground">Your information is loaded from Spring Boot.</p>{loading?<p>Loading profile...</p>:<><div className="my-8 flex items-center gap-5 border-y py-6"><span className="avatar !size-16 !text-xl">{profile.name.split(' ').map(n=>n[0]).join('')}</span><div><h2 className="section-heading">{profile.name}</h2><p className="text-xs text-muted-foreground">{profile.headline}</p></div></div><form className="max-w-[760px]" onSubmit={e=>{void save(e);}}><h3 className="section-heading">Personal information</h3><div className="mt-5 grid grid-cols-1 gap-5 sm:grid-cols-2">{(['name','email','phone','location'] as const).map(k=><label className="field" key={k}>{k==='name'?'Full name':k==='email'?'Email (account identifier)':k==='phone'?'Phone':'Location'}<input value={draft[k]??''} required={k==='name'} readOnly={k==='email'} onChange={e=>update(k,e.target.value)}/></label>)}</div><label className="field mt-5">Professional headline<input value={draft.headline} onChange={e=>update('headline',e.target.value)}/></label><label className="field mt-5">About you<textarea value={draft.bio} onChange={e=>update('bio',e.target.value)}/></label><div className="mt-5 rounded-md border p-4 text-xs"><p className="font-medium">Resume: {profile.resume||'Not uploaded'}</p><label className="mt-3 inline-flex cursor-pointer items-center gap-2 text-primary"><Upload className="size-4"/>Upload PDF/DOCX (max 5 MB)<input className="sr-only" aria-label="Upload profile resume" type="file" accept=".pdf,.docx" onChange={e=>{const file=e.target.files?.[0];if(file)void uploadProfileResume(file);}}/></label></div><div className="mt-7 flex justify-end gap-3 border-t pt-5"><Button type="button" variant="outline" onClick={()=>setDraft(profile)}>Discard</Button><Button type="submit" disabled={saving}>{saving?'Saving...':'Save profile'}</Button></div></form></>}</>;
}
