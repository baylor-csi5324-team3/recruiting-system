import { useMemo, useState, type FormEvent } from 'react';
import { ArrowUpRight, ArrowRight, Bookmark, Search, MapPin, Check, Upload, Sprout, Clock, BriefcaseBusiness } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import hero from '@/assets/opportunities.jpg';
import { useRecruiting } from './store';
import { formatDate, type Job } from './data';
import { toast } from 'sonner';

export function CompanyLogo({job}:{job:Job}) {
  return <span className={`company-logo ${job.color}`} aria-label={job.company}>{job.logo}</span>;
}
export function JobCard({job,onSelect}:{job:Job;onSelect:(job:Job)=>void}) {
  const {saved,toggleSave,role} = useRecruiting();
  return <article className="job-card">
    <div className="flex items-center justify-between">
      <div className="flex items-center gap-3"><CompanyLogo job={job}/><div><p className="text-xs font-semibold">{job.company}</p><p className="mt-1 text-[10px] text-muted-foreground">{job.posted}</p></div></div>
      {role === 'Applicant' && <Button variant="ghost" size="icon" aria-label="Save job" onClick={()=>{void toggleSave(job.id);}}><Bookmark className="size-[17px]" fill={saved.includes(job.id)?'currentColor':'none'}/></Button>}
    </div>
    <Button variant="link" onClick={()=>onSelect(job)} className="job-title mt-5 h-auto justify-start whitespace-normal p-0 text-left text-[16px] font-semibold text-foreground no-underline hover:no-underline">{job.title}</Button>
    <div className="mt-2.5 flex items-center gap-1.5 text-[11px] text-muted-foreground"><MapPin className="size-3"/>{job.location}</div>
    <div className="mt-4 flex flex-wrap gap-1.5"><span className="tag">{job.type}</span>{job.mode !== 'Not specified' && <span className="tag">{job.mode}</span>}</div>
    <div className="job-card-footer mt-5 flex items-center justify-between gap-2 border-t pt-4"><div><p className="text-xs font-semibold">{job.salary}</p><p className="mt-1 text-[9px] text-muted-foreground">Closes {formatDate(job.closes)}</p></div><Button variant="ghost" size="sm" onClick={()=>onSelect(job)} className="h-8 gap-1.5 px-1 text-[11px] text-primary">View job <ArrowUpRight className="size-3.5"/></Button></div>
  </article>;
}

export function ExploreJobs({savedOnly=false}:{savedOnly?:boolean}) {
  const {jobs,saved,role,loading,error,actor} = useRecruiting();
  const [keyword,setKeyword]=useState('');
  const [search,setSearch]=useState('');
  const [location,setLocation]=useState('');
  const [type,setType]=useState('');
  const [sort,setSort]=useState('recent');
  const [selected,setSelected]=useState<Job|null>(null);
  const options = useMemo(()=>[...new Set(jobs.map(j=>j.location))].sort(),[jobs]);
  const types = useMemo(()=>[...new Set(jobs.map(j=>j.type))].sort(),[jobs]);
  const matched = useMemo(()=>jobs.filter(j=>
    (savedOnly ? saved.includes(j.id) : j.status === 'Open') &&
    (!search || `${j.title} ${j.company}`.toLowerCase().includes(search.toLowerCase())) &&
    (!location || j.location===location) && (!type || j.type===type))
    .sort((a,b)=>sort==='closing'?a.closes.localeCompare(b.closes):b.id-a.id),
  [jobs,saved,savedOnly,search,location,type,sort]);
  const openCount=jobs.filter(j=>j.status==='Open').length;
  return <div className={`opportunities-layout ${savedOnly?'shortlist-layout':'explore-layout'}`}>
    {savedOnly ? <div className="mb-7"><p className="mb-2 text-xs text-primary">YOUR SHORTLIST</p><h1 className="page-heading">Saved opportunities</h1><p className="mt-2 text-sm text-muted-foreground">Saved jobs are loaded from the server.</p></div>
      : <><div className="explore-intro"><div><p className="text-[12px] text-muted-foreground">{role==='Guest'?'A fresh start begins here':'A little ambition goes a long way'}</p><h2 className="mt-1 text-[18px] font-semibold">{role==='Guest'?'Welcome to Verdant.':`Make your next move${actor?', '+actor.name.split(' ')[0]:''}.`}</h2></div></div>
        <section className="opportunity-banner"><img src={hero} alt="Wind turbines on green hills" width={1920} height={768}/><div className="banner-copy"><span className="banner-eyebrow"><Sprout className="size-3.5"/> THE OPPORTUNITY EDIT</span><h1>Good work.<br/>Great possibilities.</h1><p>Find your place in a team that moves you forward.</p></div><span className="banner-index"><span className="banner-index-number">{String(openCount).padStart(2,'0')}</span><span>open roles</span></span></section></>}
    <form onSubmit={e=>{e.preventDefault();setSearch(keyword);}} className="job-search"><div className="flex flex-1 items-center gap-2.5 px-3"><Search className="size-4 shrink-0 text-muted-foreground"/><input aria-label="Job title or company" placeholder="Job title or company" value={keyword} onChange={e=>setKeyword(e.target.value)} className="h-9 w-full min-w-0 bg-transparent text-xs"/></div>
      <div className="border-border px-3 sm:w-[190px] sm:border-l"><select aria-label="Location" value={location} onChange={e=>setLocation(e.target.value)} className="h-9 w-full bg-transparent text-xs"><option value="">Any location</option>{options.map(x=><option key={x} value={x}>{x}</option>)}</select></div>
      <div className="border-border px-3 sm:w-[150px] sm:border-l"><select aria-label="Employment type" value={type} onChange={e=>setType(e.target.value)} className="h-9 w-full bg-transparent text-xs"><option value="">Job type</option>{types.map(x=><option key={x}>{x}</option>)}</select></div>
      <Button type="submit" className="h-10 gap-3 text-xs">Search jobs <ArrowRight className="size-3.5"/></Button>
    </form>
    <div className="results-heading flex items-center justify-between py-5"><p className="text-[12px]"><strong>{matched.length} opportunities</strong></p><label className="text-[10px] text-muted-foreground">Sort by: <select value={sort} onChange={e=>setSort(e.target.value)} className="bg-transparent"><option value="recent">Most recent</option><option value="closing">Closing soon</option></select></label></div>
    {loading ? <p className="py-6 text-sm">Loading jobs from Spring Boot...</p> : error ? <p role="alert" className="text-destructive">{error}</p> : matched.length ? <div className="job-grid">{matched.map(j=><JobCard job={j} key={j.id} onSelect={setSelected}/>)}</div>
       : <div className="empty-state"><Search className="mx-auto mb-4 size-7"/><h3>No matching opportunities</h3><Button variant="outline" onClick={()=>{setSearch('');setKeyword('');setLocation('');setType('');}}>Clear filters</Button></div>}
    <JobDetails job={selected} onClose={()=>setSelected(null)}/>
  </div>;
}

function JobDetails({job,onClose}:{job:Job|null;onClose:()=>void}) {
  const {applications,profile,role,actor,saved,toggleSave,submit}=useRecruiting();
  const [step,setStep]=useState(0);
  const [file,setFile]=useState<File|null>(null);
  const [cover,setCover]=useState('');
  const [submitting,setSubmitting]=useState(false);
  const applied=Boolean(job && applications.some(a=>a.jobId===job.id));
  const close=()=>{if(submitting)return;setStep(0);setFile(null);setCover('');onClose();};
  const onSubmit=async(e:FormEvent<HTMLFormElement>)=>{
    e.preventDefault();
    if(step===1){if(!file){toast.error('Please select a PDF or DOCX resume.');return;}setStep(2);return;}
    if(!job || submitting) return;
    setSubmitting(true);
    try{const ok=await submit(job,file,cover);if(ok)closeAfterSubmit();}
    finally{setSubmitting(false);}
  };
  const closeAfterSubmit=()=>{setStep(0);setFile(null);setCover('');onClose();};
  return <Dialog open={Boolean(job)} onOpenChange={o=>{if(!o)close();}}><DialogContent className="max-h-[90vh] max-w-[660px] overflow-y-auto">{job && <>
    <div className="flex items-center gap-3"><CompanyLogo job={job}/><div><p className="text-xs text-muted-foreground">{job.company}</p><DialogTitle className="mt-1 text-xl">{step?'Apply for ':''}{job.title}</DialogTitle></div></div>
    <DialogDescription>{job.location} · {job.type}</DialogDescription>
    {step===0 ? <><div className="flex gap-2"><span className="tag">{job.salary}</span><span className={`status ${job.status.toLowerCase()}`}>{job.status}</span></div>
      <section className="mt-3"><h3 className="section-heading">About the role</h3><p className="mt-3 text-sm leading-7 text-muted-foreground">{job.description}</p><p className="mt-5 flex items-center gap-2 text-xs text-muted-foreground"><Clock className="size-4"/>Applications close {formatDate(job.closes)}</p></section>
      <div className="mt-4 flex flex-wrap gap-3 border-t pt-5"><Button disabled={applied || job.status!=='Open' || !actor || role!=='Applicant'} onClick={()=>{setFile(null);setStep(1);}}>{applied?<><Check/>Already applied</>:job.status!=='Open'?'Applications closed':<>Apply now<ArrowUpRight/></>}</Button>{role==='Applicant'&&<Button variant="outline" onClick={()=>{void toggleSave(job.id);}}><Bookmark/>{saved.includes(job.id)?'Saved':'Save job'}</Button>}</div>
    </> : <form className="space-y-4" onSubmit={e=>{void onSubmit(e);}}><div className="flex items-center gap-3 border-y py-3 text-xs"><span className={step===1?'font-semibold text-primary':'text-muted-foreground'}>1. Application materials</span><ArrowRight className="size-3"/><span className={step===2?'font-semibold text-primary':'text-muted-foreground'}>2. Review & submit</span></div>
      {step===1?<><div className="grid grid-cols-1 gap-3 sm:grid-cols-2"><label className="field">Full name<input readOnly value={profile.name}/></label><label className="field">Email<input readOnly value={profile.email}/></label></div>
        <div className="field">Resume<label className="cursor-pointer rounded-md border border-dashed p-5 text-center"><Upload className="mx-auto mb-2 size-5 text-primary"/><span className="block text-xs">{file?.name||'Choose resume'}</span><span className="text-[10px] text-muted-foreground">PDF or DOCX · max 5 MB</span><input type="file" className="sr-only" accept=".pdf,.docx" aria-label="Upload application resume" onChange={e=>{const f=e.target.files?.[0];if(!f)return;if(!/\.(pdf|docx)$/i.test(f.name)||f.size===0||f.size>5*1024*1024){toast.error('Invalid resume (max 5 MB)');setFile(null);return;}setFile(f);}}/></label></div><label className="field">Cover letter (optional)<textarea value={cover} onChange={e=>setCover(e.target.value)}/></label>
        </>:<><p className="text-xs">Applicant: {profile.name} · {profile.email}</p><p className="flex items-center gap-2 text-xs"><BriefcaseBusiness className="size-4"/>{file?.name}</p><label className="flex items-start gap-2 text-xs"><input required type="checkbox"/>I confirm that my information is accurate.</label></>}
      <div className="flex justify-between border-t pt-4"><Button type="button" variant="ghost" disabled={submitting} onClick={()=>setStep(step-1)}>Back</Button><Button type="submit" disabled={submitting}>{submitting?'Submitting...':step===1?'Review application':'Submit application'}<ArrowRight/></Button></div>
    </form>}</>}</DialogContent></Dialog>;
}
