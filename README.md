# cloud-itonami-isco-7115

Open Occupation Blueprint for **ISCO-08 7115**: Carpenters and Joiners.

This repository designs a forkable OSS business for an independent carpenter: a cutting-support and material-handling robot performs measurement and panel-positioning tasks under a governor-gated actor, so the practice keeps its own job and safety records instead of renting a closed trades-management SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a cutting-support and material-handling robot performs precision measurement and panel positioning under an actor that proposes
actions and an independent **Carpentry Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
operating near powered saws, or working at height) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
job order + material list + safety plan
        |
        v
Carpentry Advisor -> Carpentry Governor -> assemble/finish, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `7115`). Required capabilities:

- :robotics
- :forms
- :telemetry
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
