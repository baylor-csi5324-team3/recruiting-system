# Documentation

Documentation evolves together with the implementation. When a change alters
behaviour, update the affected document in the same pull request.

| Folder | Contents |
|---|---|
| `requirements/` | Vision and scope, functional and nonfunctional requirements, business rules, user stories, use cases, traceability matrix |
| `design/` | Architecture description, UML models (use case, class, sequence, state), database design, wireframes |
| `meeting-logs/` | Dated records of team meetings, decisions, and action items (maintained by the Project Librarian) |

## Traceability

Each requirement should be traceable through to design, implementation, and tests.
Link artifacts using stable identifiers, for example `UC-07`, and reference that
identifier in the corresponding GitHub issue, branch name, and test class.

## Iteration Milestones

| Iteration | Week | Focus |
|---|---|---|
| 1 | 6 | Requirements and analysis; repository and issue tracking; initial prototype |
| 2 | 10 | Architecture and detailed design; working prototype; database design; automated testing |
| 3 | 15/16 | Complete implementation; deployment; final documentation; demonstration |
