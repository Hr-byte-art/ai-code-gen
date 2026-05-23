<template>
  <footer class="footer">
    <div class="footer-content">
      <div class="footer-brand">
        <div class="footer-logo">
          <img src="@/assets/logo.svg" alt="Logo" class="logo-img" />
          <span class="brand-name">熵擎 AI</span>
        </div>
        <p class="footer-slogan">AI 驱动的智能应用生成平台</p>
      </div>
      <div class="footer-bottom">
        <span class="copyright">&copy; 2024 熵擎 AI</span>
        <span class="dot">&middot;</span>
        <a href="mailto:contact@entropy-ai.com" class="contact-link">contact@entropy-ai.com</a>
        <span class="dot">&middot;</span>
        <span class="ai-badge">
          <span class="ai-dot"></span>
          AI 技术驱动
        </span>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
</script>

<style scoped>
.footer {
  background: rgba(255, 255, 255, 0.85);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
  backdrop-filter: blur(16px) saturate(180%);
  border-top: 1px solid rgba(226, 232, 240, 0.8);
  padding: 28px 0;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  text-align: center;
}

.footer-brand {
  margin-bottom: 16px;
}

.footer-logo {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.logo-img {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  opacity: 0.85;
}

.brand-name {
  font-size: 15px;
  font-weight: 700;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.footer-slogan {
  color: #64748b;
  font-size: 13px;
  margin: 0;
  font-weight: 400;
}

.footer-bottom {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 12px;
  color: #94a3b8;
}

.dot {
  color: #cbd5e1;
}

.contact-link {
  color: #64748b;
  text-decoration: none;
  font-weight: 500;
  transition: color var(--transition-fast);
}

.contact-link:hover {
  color: var(--color-primary);
}

.ai-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.ai-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  display: inline-block;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.8); }
}

@media (max-width: 768px) {
  .footer {
    padding: 20px 0;
  }
  .footer-bottom {
    flex-direction: column;
    gap: 6px;
  }
  .dot {
    display: none;
  }
}
</style>
