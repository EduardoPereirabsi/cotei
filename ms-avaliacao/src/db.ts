import mongoose from "mongoose";

/**
 * Conecta ao MongoDB tentando de novo ate conseguir.
 * O servidor HTTP sobe antes; a readiness probe (/avaliacao/health) so libera
 * trafego quando a conexao existe. Assim o pod nao reinicia em laco enquanto
 * o banco ainda esta subindo.
 */
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
