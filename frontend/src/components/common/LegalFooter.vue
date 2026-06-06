<template>
  <footer :class="['legal-footer', variant]">
    <div class="legal-footer-inner">
      <div class="legal-brand-row">
        <span class="legal-brand">个人熵擎Ai开发</span>
        <span class="legal-dot" />
        <span class="legal-desc">需求、生成、迭代、部署一站式工作流</span>
      </div>
      <div class="legal-meta-row">
        <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer" class="legal-link">
          蜀ICP备2025160153号-2
        </a>
        <a href="http://entropyqing.asia/" target="_blank" rel="noopener noreferrer" class="legal-link">
          www.entropyqing.asia
        </a>
        <span class="legal-copy">© 2026</span>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ variant?: 'brand' | 'compact' }>(), {
  variant: 'brand'
})
</script>

<style scoped>
.legal-footer {
  color: var(--t-light);
}

.legal-footer.brand {
  border-top: 1px solid var(--border-light);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.7), rgba(248, 246, 241, 0.92)),
    var(--bg-card);
  padding: 18px 0 20px;
}

.legal-footer.compact {
  margin-top: 24px;
  padding: 16px 0 4px;
}

.legal-footer-inner {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  font-size: 12px;
}

.legal-brand-row,
.legal-meta-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.legal-brand {
  color: var(--t-secondary);
  font-weight: 800;
  letter-spacing: -0.1px;
}

.legal-dot {
  width: 4px;
  height: 4px;
  border-radius: 999px;
  background: var(--c-primary-200);
}

.legal-desc,
.legal-domain,
.legal-copy {
  color: var(--t-light);
}

.legal-link {
  color: var(--t-muted);
  text-decoration: none;
  transition: color var(--t-fast), background var(--t-fast);
}

.legal-link:hover {
  color: var(--c-primary);
}

.legal-footer.compact .legal-footer-inner {
  max-width: none;
  padding: 0;
}

.legal-footer.compact .legal-brand-row {
  display: none;
}

.legal-footer.compact .legal-meta-row {
  width: 100%;
  justify-content: center;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}

@media (max-width: 768px) {
  .legal-footer-inner {
    align-items: flex-start;
    flex-direction: column;
    padding: 0 16px;
  }

  .legal-footer.compact .legal-meta-row {
    justify-content: flex-start;
  }
}
</style>