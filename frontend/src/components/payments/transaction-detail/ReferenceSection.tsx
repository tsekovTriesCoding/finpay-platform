import { Copy } from 'lucide-react';

interface ReferenceSectionProps {
  reference: string;
}

export default function ReferenceSection({ reference }: ReferenceSectionProps) {
  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(reference);
    } catch {
      // Fallback - noop in non-secure contexts
    }
  };

  return (
    <div className="bg-dark-800/60 rounded-2xl border border-dark-700/50 px-4 py-3">
      <p className="text-xs text-dark-400 mb-1">Transaction Reference</p>
      <div className="flex items-center justify-between gap-2">
        <code className="text-xs text-dark-300 font-mono truncate">{reference}</code>
        <button
          onClick={handleCopy}
          className="shrink-0 p-1.5 rounded-lg bg-dark-700 hover:bg-dark-600 text-dark-400 hover:text-white transition-colors"
          title="Copy reference"
        >
          <Copy className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
}
