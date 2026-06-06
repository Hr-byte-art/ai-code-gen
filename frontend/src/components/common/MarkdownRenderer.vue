<template>
  <pre v-if="usePlainText" class="markdown-plain">{{ content }}</pre>
  <div v-else class="markdown-body" v-html="renderedHtml" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js/lib/core'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
import css from 'highlight.js/lib/languages/css'
import json from 'highlight.js/lib/languages/json'
import sql from 'highlight.js/lib/languages/sql'
import bash from 'highlight.js/lib/languages/bash'
import python from 'highlight.js/lib/languages/python'
import java from 'highlight.js/lib/languages/java'

hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('js', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('ts', typescript)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('html', xml)
hljs.registerLanguage('css', css)
hljs.registerLanguage('json', json)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('shell', bash)
hljs.registerLanguage('python', python)
hljs.registerLanguage('java', java)

const renderCodeBlock = (code: string, lang?: string) => {
  const language = lang?.trim()
  const hasLanguage = !!language && hljs.getLanguage(language)
  const highlighted = hasLanguage
    ? hljs.highlight(code, { language }).value
    : hljs.highlightAuto(code).value
  const languageClass = hasLanguage ? ` class="language-${language}"` : ''

  return `<pre><code${languageClass}>${highlighted}</code></pre>`
}

marked.use({
  gfm: true,
  breaks: true,
  renderer: {
    code({ text, lang }) {
      return renderCodeBlock(text, lang)
    },
  },
})

const props = defineProps<{ content: string }>()
const PLAIN_TEXT_THRESHOLD = 40000

const usePlainText = computed(() => props.content.length > PLAIN_TEXT_THRESHOLD)

const renderedHtml = computed(() => {
  if (!props.content || usePlainText.value) return ''
  return marked.parse(props.content) as string
})
</script>

<style>
.markdown-body {
  font-size: 14px;
  line-height: 1.75;
  color: var(--t-primary);
  word-break: break-word;
}

.markdown-plain {
  max-height: 58vh;
  margin: 0;
  padding: 12px 14px;
  overflow: auto;
  border: 1px solid var(--border-light);
  border-radius: var(--r-lg);
  background: var(--bg-soft);
  color: var(--t-primary);
  font-family: 'JetBrains Mono', 'Fira Code', 'Cascadia Code', Consolas, monospace;
  font-size: 12px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}
.markdown-body h1, .markdown-body h2, .markdown-body h3,
.markdown-body h4, .markdown-body h5, .markdown-body h6 {
  margin: 1em 0 0.5em;
  font-weight: 700;
  line-height: 1.3;
  color: var(--t-primary);
}
.markdown-body h1 { font-size: 1.5em; }
.markdown-body h2 { font-size: 1.3em; }
.markdown-body h3 { font-size: 1.15em; }
.markdown-body p { margin: 0.5em 0; }
.markdown-body ul, .markdown-body ol {
  padding-left: 1.5em;
  margin: 0.5em 0;
}
.markdown-body li { margin: 0.25em 0; }
.markdown-body blockquote {
  margin: 0.5em 0;
  padding: 0.5em 1em;
  border-left: 3px solid var(--c-primary-200);
  background: var(--bg-soft);
  border-radius: 0 var(--r-sm) var(--r-sm) 0;
  color: var(--t-secondary);
}
.markdown-body table {
  width: 100%;
  border-collapse: collapse;
  margin: 0.75em 0;
  font-size: 13px;
}
.markdown-body th, .markdown-body td {
  padding: 8px 12px;
  border: 1px solid var(--border-light);
  text-align: left;
}
.markdown-body th {
  background: var(--bg-soft);
  font-weight: 700;
}
.markdown-body a {
  color: var(--c-primary);
  text-decoration: none;
}
.markdown-body a:hover { text-decoration: underline; }
.markdown-body strong { font-weight: 700; }
.markdown-body hr {
  margin: 1em 0;
  border: none;
  border-top: 1px solid var(--border-light);
}

/* 行内 code */
.markdown-body code {
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--bg-soft);
  border: 1px solid var(--border-light);
  font-family: 'JetBrains Mono', 'Fira Code', 'Cascadia Code', Consolas, monospace;
  font-size: 0.9em;
  color: #c7254e;
}

/* 代码块 */
.markdown-body pre {
  margin: 0.75em 0;
  border-radius: var(--r-lg);
  overflow: hidden;
  border: 1px solid #2d2d2d;
}
.markdown-body pre code {
  display: block;
  padding: 14px 18px;
  background: #1e1e2e;
  color: #cdd6f4;
  font-size: 13px;
  line-height: 1.6;
  overflow-x: auto;
  border: none;
  border-radius: 0;
}

/* highlight.js Catppuccin Mocha 主题 */
.hljs-keyword, .hljs-selector-tag, .hljs-built_in { color: #cba6f7; }
.hljs-string, .hljs-attr { color: #a6e3a1; }
.hljs-number, .hljs-literal { color: #fab387; }
.hljs-comment { color: #6c7086; font-style: italic; }
.hljs-function .hljs-title, .hljs-title.function_ { color: #89b4fa; }
.hljs-type, .hljs-class .hljs-title { color: #f9e2af; }
.hljs-variable, .hljs-template-variable { color: #f38ba8; }
.hljs-tag { color: #cba6f7; }
.hljs-name { color: #f38ba8; }
.hljs-attribute { color: #89dceb; }
.hljs-symbol, .hljs-bullet { color: #f5c2e7; }
.hljs-meta { color: #fab387; }
.hljs-deletion { color: #f38ba8; background: rgba(243,139,168,0.1); }
.hljs-addition { color: #a6e3a1; background: rgba(166,227,161,0.1); }
</style>
