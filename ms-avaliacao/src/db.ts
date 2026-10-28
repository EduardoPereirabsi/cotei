import mongoose from "mongoose";

// Retenta ate conectar; ate la, a readiness (/avaliacao/health) segura o trafego.
export async function conectarComRetentativa(url: string, intervaloMs = 3000): Promise<void> {
  for (let tentativa = 1; ; tentativa++) {
    try {
      await mongoose.connect(url, { serverSelectionTimeoutMS: 3000 });
      console.log(`[MONGO] conectado (tentativa ${tentativa})`);
      return;
    } catch {
      console.warn(`[MONGO] indisponivel (tentativa ${tentativa}), nova tentativa em ${intervaloMs / 1000}s`);
      await new Promise((resolve) => setTimeout(resolve, intervaloMs));
    }
  }
}

export function mongoConectado(): boolean {
  return mongoose.connection.readyState === 1;
}
