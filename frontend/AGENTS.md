<!-- LOVABLE:BEGIN -->
> [!IMPORTANT]
> This project is connected to [Lovable](https://lovable.dev). Avoid rewriting
> published git history — force pushing, or rebasing/amending/squashing commits
> that are already pushed — as it rewrites history on Lovable's side and the
> user will likely lose their project history.
>
> Commits you push to the connected branch sync back to Lovable and show up in
> the editor, so keep the branch in a working state.
<!-- LOVABLE:END -->

## Application rules
- Use TanStack file routes for each workspace screen; share navigation through the root layout so direct URLs and browser history remain coherent.
- Keep sample recruiting state in a React context and domain data in a separate module; this is a frontend prototype intended for later integration with the team's application service.
- Keep role-sensitive actions and allowed stage changes centralized in the recruiting state module so screens share the same workflow constraints; frontend checks are not production authorization.
- Define visual roles in global semantic tokens and use existing UI primitives for controls so the interface remains consistent.

- Keep translucent layout treatments in shared semantic CSS tokens and layout classes; job browsing uses an editorial lead card and an overlapping search band without changing recruiting state.
