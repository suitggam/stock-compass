import type { ReactNode } from "react";

export type SummaryStatItem = {
  id: string;
  label: string;
  value: string;
  helper?: string;
  tone?: "default" | "positive" | "negative";
  icon?: ReactNode;
};

const toneClass = (tone: SummaryStatItem["tone"]) => {
  switch (tone) {
    case "positive":
      return "text-emerald-500";
    case "negative":
      return "text-rose-500";
    default:
      return "text-slate-400";
  }
};

export default function SummaryStats({ items }: { items: SummaryStatItem[] }) {
  return (
    <section className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-5">
      {items.map((item) => (
        <article
          key={item.id}
          className="rounded-2xl bg-white/80 px-4 py-3 shadow-sm backdrop-blur-sm ring-1 ring-white/50"
        >
          <p className="text-[13px] font-medium text-slate-500">{item.label}</p>
          <p className="mt-2 text-lg font-semibold text-slate-900">{item.value}</p>
          {item.helper ? (
            <p className={`mt-1 text-xs font-medium ${toneClass(item.tone)}`}>{item.helper}</p>
          ) : null}
        </article>
      ))}
    </section>
  );
}
