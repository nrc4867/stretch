package dev.chieppa.stretch.markdown

/** A pull request description as Dependabot writes it: nested details with HTML and markdown in them. */
internal val dependabotDescription = """
Bumps [actions/checkout](https://github.com/actions/checkout) from 3 to 4.
<details>
<summary>Release notes</summary>
<p><em>Sourced from <a href="https://github.com/actions/checkout/releases">actions/checkout's releases</a>.</em></p>
<blockquote>
<h2>v4.0.0</h2>
<ul>
<li>Update default runtime to node20 by <a href="https://github.com/takost"><code>@​takost</code></a> in <a href="https://github.com/actions/checkout/pull/1436">actions/checkout#1436</a></li>
</ul>
</blockquote>
</details>
<details>
<summary>Commits</summary>
<ul>
<li><a href="https://github.com/actions/checkout/commit/3df4ab1"><code>3df4ab1</code></a> Release 4.0.0 (<a href="https://github.com/actions/checkout/issues/1447">#1447</a>)</li>
<li>See full diff in <a href="https://github.com/actions/checkout/compare/v3...v4">compare view</a></li>
</ul>
</details>
<br />


[![Dependabot compatibility score](https://dependabot-badges.githubapp.com/badges/compatibility_score?dependency-name=actions/checkout)](https://docs.github.com/en/code-security/dependabot)

Dependabot will resolve any conflicts with this PR as long as you don&#39;t alter it yourself. You can also trigger a rebase manually by commenting `@dependabot rebase`.

[//]: # (dependabot-automerge-start)
[//]: # (dependabot-automerge-end)

---

<details>
<summary>Dependabot commands and options</summary>
<br />

You can trigger Dependabot actions by commenting on this PR:
- `@dependabot rebase` will rebase this PR
- `@dependabot recreate` will recreate this PR, overwriting any edits that have been made to it

</details>
""".trimStart()

/** The address of the picture in [dependabotDescription]. */
internal const val DEPENDABOT_BADGE = "https://dependabot-badges.githubapp.com/badges/compatibility_score?dependency-name=actions/checkout"
