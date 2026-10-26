import { useState, type FormEvent } from 'react';
import { Plus, ArrowUpRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { useRecruiting } from './store';
import { formatDate, type Job, type Role } from './data';
import { toast } from 'sonner';

export function PostingsPage() {
  const {jobs,applications,role,actor,saveJob,closeJob,loading,error}=useRecruiting();
  const [tab,setTab]=useState('All postings');
  const [editor,setEditor]=useState<Job|'new'|null>(null);
  const [closing,setClosing]=useState<Job|null>(null);
  const [working,setWorking]=useState(false);
  const own=jobs.filter(j=>role==='Recruiter' && j.company===actor?.company);
  const empty:Job={id:-1,title:'',company:actor?.company??'',logo:'',color:'green',location:'',type:'Full-time',mode:'Not specified',salary:'Not disclosed',category:'Not specified',level:'Not specified',posted:'Not published',closes:'',status:'Draft',description:''};
  const filtered=own.filter(j=>tab==='All postings'||j.status===tab);
  return <><div className="flex flex-wrap items-end justify-between gap-4"><div><p className="mb-2 text-[10px] font-semibold uppercase tracking-[1.4px] text-primary">{actor?.company??'RECRUITER'} · HIRING WORKSPACE</p><h1 className="page-heading">Build your next great team.</h1><p className="mt-2 text-sm text-muted-foreground">Postings are stored in Spring Boot.</p></div><Button disabled={!actor} onClick={()=>setEditor('new')}><Plus/>Create posting</Button></div>
    <div className="my-8 grid grid-cols-3 border-y py-6">{['Open','Draft','Closed'].map((s,i)=><div key={s} className={i?'border-l pl-5':''}><p className="text-xs text-muted-foreground">{s} postings</p><p className="mt-2 font-display text-3xl font-semibold">{String(own.filter(j=>j.status===s).length).padStart(2,'0')}</p></div>)}</div>
    <div className="mb-5 flex flex-wrap gap-1">{['All postings','Open','Draft','Closed'].map(t=><Button variant={tab===t?'secondary':'ghost'} size="sm" key={t} onClick={()=>setTab(t)}>{t}</Button>)}</div>
    {loading?<p className="text-sm">Loading postings...</p>:error?<p className="text-sm text-destructive">{error}</p>:<div className="overflow-x-auto rounded-md border bg-card"><table className="data-table"><thead><tr><th>Job title</th><th>Status</th><th>Applicants</th><th>Closing date</th><th></th></tr></thead><tbody>{filtered.map(j=><tr key={j.id}><td><p className="font-semibold">{j.title}</p><p className="text-[11px] text-muted-foreground">{j.location} · {j.type}</p></td><td><span className={`status ${j.status.toLowerCase()}`}>{j.status}</span></td><td>{applications.filter(a=>a.jobId===j.id).length}</td><td>{formatDate(j.closes)}</td><td><div className="flex gap-2">{j.status!=='Closed'&&<Button variant="outline" size="sm" onClick={()=>setEditor(j)}>Edit</Button>}{j.status==='Open'&&<Button variant="ghost" size="sm" onClick={()=>setClosing(j)}>Close</Button>}</div></td></tr>)}</tbody></table>{!filtered.length&&<div className="empty-state">No postings in this view.</div>}</div>}
    <Dialog open={Boolean(editor)} onOpenChange={o=>{if(!o)setEditor(null);}}><DialogContent className="max-h-[90vh] max-w-[680px] overflow-y-auto"><DialogTitle>{editor==='new'?'Create job posting':'Edit job posting'}</DialogTitle><DialogDescription>Save the posting in the backend.</DialogDescription>{editor&&<PostingForm key={editor==='new'?'new':editor.id} job={editor==='new'?empty:editor} onSave={async j=>{setWorking(true);const ok=await saveJob(j);setWorking(false);if(ok)setEditor(null);}} onCancel={()=>setEditor(null)} working={working}/>}</DialogContent></Dialog>
    <Dialog open={Boolean(closing)} onOpenChange={o=>{if(!o)setClosing(null);}}><DialogContent><DialogTitle>Close this posting?</DialogTitle><DialogDescription>New applications will not be accepted.</DialogDescription><div className="flex justify-end gap-2"><Button variant="outline" onClick={()=>setClosing(null)}>Keep open</Button><Button onClick={()=>{if(closing)void closeJob(closing).then(ok=>{if(ok)setClosing(null);});}}>Close posting</Button></div></DialogContent></Dialog>
  </>;
}
function PostingForm({job,onSave,onCancel,working}:{job:Job;onSave:(j:Job)=>Promise<void>;onCancel:()=>void;working:boolean}) {
  const [draft,setDraft]=useState(job);
  const [action,setAction]=useState<'Draft'|'Open'>(job.status==='Open'?'Open':'Draft');
  const edit=(key:keyof Job,value:string)=>setDraft(d=>({...d,[key]:value}));
  const save=(e:FormEvent<HTMLFormElement>)=>{e.preventDefault();if(draft.closes<=new Date().toISOString().slice(0,10)){toast.error('Closing date must be in the future');return;}void onSave({...draft,status:action});};
  return <form className="space-y-4" onSubmit={save}><label className="field">Job title<input required value={draft.title} onChange={e=>edit('title',e.target.value)}/></label><div className="grid grid-cols-1 gap-4 sm:grid-cols-2"><label className="field">Location<input required value={draft.location} onChange={e=>edit('location',e.target.value)}/></label><label className="field">Closing date<input required type="date" value={draft.closes} onChange={e=>edit('closes',e.target.value)}/></label><label className="field">Employment type<select value={draft.type} onChange={e=>edit('type',e.target.value)}>{['Full-time','Part-time','Contract','Internship'].map(x=><option key={x}>{x}</option>)}</select></label></div><label className="field">Description<textarea required value={draft.description} onChange={e=>edit('description',e.target.value)}/></label><p className="text-xs text-muted-foreground">Salary, work mode, category and experience level are not available in the backend model; they are intentionally omitted rather than saved only in memory.</p><div className="flex flex-wrap justify-end gap-2 border-t pt-4"><Button type="button" variant="ghost" onClick={onCancel}>Cancel</Button>{job.status==='Draft'&&<Button type="submit" variant="outline" disabled={working} onClick={()=>setAction('Draft')}>Save draft</Button>}<Button type="submit" disabled={working} onClick={()=>setAction('Open')}>Publish posting <ArrowUpRight/></Button></div></form>;
}
export function AccountsPage() {
  const {accounts,role,updateAccount,loading,error}=useRecruiting();
  const [search,setSearch]=useState('');
  const [pending,setPending]=useState<{id:number;active?:boolean;role?:Role}|null>(null);
  const [saving,setSaving]=useState(false);
  const selected=accounts.find(a=>a.id===pending?.id);
  if(role!=='Administrator')return <div className="empty-state">Choose the administrator workspace.</div>;
  const apply=async()=>{
    if(!pending)return;
    setSaving(true);
    const ok=await updateAccount(pending.id,pending);
    setSaving(false);if(ok)setPending(null);
  };
  return <><p className="mb-2 text-[10px] font-semibold uppercase tracking-[1.4px] text-primary">SYSTEM ADMINISTRATION</p><h1 className="page-heading">People & permissions</h1><p className="mt-2 text-sm text-muted-foreground">Accounts are loaded from the Spring Boot database.</p>
    <div className="my-7 flex flex-wrap items-center justify-between gap-3 border-y py-5"><p className="text-sm"><strong>{accounts.length}</strong> registered accounts <span className="ml-2 text-xs text-muted-foreground">{accounts.filter(a=>a.active).length} active</span></p><input className="form-input max-w-[280px]" aria-label="Search accounts" value={search} onChange={e=>setSearch(e.target.value)} placeholder="Search by name or email"/></div>
    {loading?<p>Loading accounts...</p>:error?<p role="alert" className="text-destructive">{error}</p>:<div className="overflow-x-auto rounded-md border bg-card"><table className="data-table"><thead><tr><th>User</th><th>Role</th><th>Status</th><th>Access</th></tr></thead><tbody>{accounts.filter(a=>`${a.name} ${a.email}`.toLowerCase().includes(search.toLowerCase())).map(a=><tr key={a.id}><td><p className="font-medium">{a.name}</p><p className="text-[11px] text-muted-foreground">{a.email}</p></td><td><select className="form-input" aria-label={`Role for ${a.name}`} value={a.role} onChange={e=>setPending({id:a.id,role:e.target.value as Role})}><option>Applicant</option><option>Recruiter</option><option>Administrator</option></select></td><td><span className={`status ${a.active?'open':''}`}>{a.active?'Active':'Inactive'}</span></td><td><Button variant="outline" size="sm" onClick={()=>setPending({id:a.id,active:!a.active})}>{a.active?'Deactivate':'Activate'}</Button></td></tr>)}</tbody></table></div>}
    <Dialog open={Boolean(pending)} onOpenChange={o=>{if(!o)setPending(null);}}><DialogContent><DialogTitle>Update account access?</DialogTitle><DialogDescription>Apply the change to {selected?.email} in the backend?</DialogDescription><div className="flex justify-end gap-2"><Button variant="outline" onClick={()=>setPending(null)}>Cancel</Button><Button disabled={saving} onClick={()=>{void apply();}}>Confirm change</Button></div></DialogContent></Dialog>
  </>;
}
