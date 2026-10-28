import { Schema, model } from "mongoose";

const AvaliacaoSchema = new Schema(
  {
    produtoId: { type: Number, required: true, index: true },
    autor: { type: String, required: true, trim: true, maxlength: 100 },
    nota: { type: Number, required: true, min: 1, max: 5 },
    comentario: { type: String, trim: true, maxlength: 1000 },
  },
  {
    timestamps: { createdAt: "criadaEm", updatedAt: false },
    versionKey: false,
    toJSON: {
      transform: (_doc, ret: Record<string, unknown>) => {
        ret.id = ret._id;
        delete ret._id;
        return ret;
      },
    },
  }
);

export const Avaliacao = model("Avaliacao", AvaliacaoSchema, "avaliacoes");
