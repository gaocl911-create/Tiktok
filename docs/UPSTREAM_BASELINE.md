# Upstream Framework Baseline

This project imports the following upstream frameworks as source code. Their
nested Git metadata was removed so the complete project is managed by the root
repository.

## Backend

- Project: RuoYi-Vue-Plus
- Repository: https://gitee.com/dromara/RuoYi-Vue-Plus.git
- Tag: `v5.6.1`
- Commit: `6bfdcae06eaf218c4204382de277499be6c88c1b`
- Local directory: `server/`

## Management Frontend

- Project: plus-ui
- Repository: https://gitee.com/JavaLionLi/plus-ui.git
- Tag: `v5.6.1-v2.6.1`
- Commit: `9fd2b6f137298ad3511ffd1816bea60d69c795ce`
- Local directory: `web/`

## Upgrade Rule

Do not pull an upstream development branch directly into this repository.
Review upstream release notes, compare the pinned tag, and merge upgrades as a
separate change with build and regression verification.

