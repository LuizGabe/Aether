import React from "react";
import { createRoot } from "react-dom/client";
import { initializeApi } from "./api";
import App from "./App";
import "./styles.css";

void initializeApi().then(() => createRoot(document.getElementById("root")!).render(<App />)).catch(() => {
  const root = document.getElementById("root");
  if (root) root.textContent = "Não foi possível abrir o perfil do Aether. Feche esta janela e confira a pasta e a porta do perfil.";
});
