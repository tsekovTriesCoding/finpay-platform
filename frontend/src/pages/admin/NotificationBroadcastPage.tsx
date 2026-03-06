import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Loader2, AlertCircle, CheckCircle2 } from 'lucide-react';

type Channel = 'IN_APP' | 'EMAIL' | 'PUSH';

export default function NotificationBroadcastPage() {
  const [subject, setSubject] = useState('');
  const [content, setContent] = useState('');
  const [channel, setChannel] = useState<Channel>('IN_APP');
  const [targetAudience, setTargetAudience] = useState<'ALL' | 'ROLE'>('ALL');
  const [targetRole, setTargetRole] = useState<'USER' | 'MERCHANT' | 'ADMIN'>('USER');
  const [sending, setSending] = useState(false);
  const [result, setResult] = useState<{ ok: boolean; message: string } | null>(null);

  const canSend = subject.trim() && content.trim();

  async function handleSend() {
    if (!canSend) return;
    setSending(true);
    setResult(null);

    try {
      // For now this shows a simulated success - wire to a real backend broadcast endpoint
      // when the notification-service admin API is built out.
      await new Promise((r) => setTimeout(r, 1200));
      setResult({
        ok: true,
        message: `Broadcast queued to ${targetAudience === 'ALL' ? 'all users' : `users with role ${targetRole}`} via ${channel}.`,
      });
      setSubject('');
      setContent('');
    } catch {
      setResult({ ok: false, message: 'Failed to send broadcast notification.' });
    } finally {
      setSending(false);
    }
  }

  return (
    <div className="space-y-6 max-w-2xl">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <h1 className="text-2xl font-bold text-white">Notification Broadcast</h1>
        <p className="text-dark-400 mt-1">Send system-wide or role-targeted notifications</p>
      </motion.div>

      <AnimatePresence>
        {result && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className={`rounded-lg px-4 py-3 text-sm flex items-center gap-3 ${
              result.ok
                ? 'bg-green-500/10 border border-green-500/30 text-green-300'
                : 'bg-red-500/10 border border-red-500/30 text-red-300'
            }`}
          >
            {result.ok
              ? <CheckCircle2 className="w-5 h-5 flex-shrink-0" />
              : <AlertCircle className="w-5 h-5 flex-shrink-0" />}
            {result.message}
          </motion.div>
        )}
      </AnimatePresence>

      <div className="bg-dark-800 border border-dark-700 rounded-xl p-6 space-y-5">
        {/* Channel */}
        <div>
          <label className="block text-sm font-medium text-dark-300 mb-1.5">Channel</label>
          <div className="flex gap-3">
            {(['IN_APP', 'EMAIL', 'PUSH'] as Channel[]).map((ch) => (
              <button
                key={ch}
                onClick={() => setChannel(ch)}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                  channel === ch
                    ? 'bg-primary-600 text-white'
                    : 'bg-dark-700 text-dark-400 hover:bg-dark-600 hover:text-white'
                }`}
              >
                {ch.replace('_', ' ')}
              </button>
            ))}
          </div>
        </div>

        {/* Target audience */}
        <div>
          <label className="block text-sm font-medium text-dark-300 mb-1.5">Target Audience</label>
          <div className="flex items-center gap-4">
            <label className="flex items-center gap-2 text-dark-400">
              <input
                type="radio"
                name="audience"
                checked={targetAudience === 'ALL'}
                onChange={() => setTargetAudience('ALL')}
                className="accent-primary-500"
              />
              All Users
            </label>
            <label className="flex items-center gap-2 text-dark-400">
              <input
                type="radio"
                name="audience"
                checked={targetAudience === 'ROLE'}
                onChange={() => setTargetAudience('ROLE')}
                className="accent-primary-500"
              />
              By Role
            </label>
            {targetAudience === 'ROLE' && (
              <select
                value={targetRole}
                onChange={(e) => setTargetRole(e.target.value as typeof targetRole)}
                className="bg-dark-700 border border-dark-600 rounded-lg px-3 py-1.5 text-dark-300 text-sm"
              >
                <option value="USER">USER</option>
                <option value="MERCHANT">MERCHANT</option>
                <option value="ADMIN">ADMIN</option>
              </select>
            )}
          </div>
        </div>

        {/* Subject */}
        <div>
          <label className="block text-sm font-medium text-dark-300 mb-1.5">Subject</label>
          <input
            type="text"
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
            placeholder="e.g. Scheduled maintenance this weekend"
            className="w-full bg-dark-700 border border-dark-600 rounded-lg px-4 py-2.5 text-white
                       placeholder-dark-500 focus:ring-2 focus:ring-primary-500 focus:border-transparent"
          />
        </div>

        {/* Content */}
        <div>
          <label className="block text-sm font-medium text-dark-300 mb-1.5">Message Body</label>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={6}
            placeholder="Write your broadcast message here..."
            className="w-full bg-dark-700 border border-dark-600 rounded-lg px-4 py-2.5 text-white
                       placeholder-dark-500 focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-y"
          />
        </div>

        {/* Preview */}
        {canSend && (
          <div className="border border-dark-600 rounded-lg p-4">
            <p className="text-xs text-dark-500 uppercase tracking-wide mb-2">Preview</p>
            <p className="text-white font-medium">{subject}</p>
            <p className="text-dark-400 text-sm mt-1 whitespace-pre-wrap">{content}</p>
          </div>
        )}

        {/* Send */}
        <button
          disabled={!canSend || sending}
          onClick={handleSend}
          className="w-full py-3 rounded-lg text-white font-medium transition-colors
                     bg-primary-600 hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed
                     flex items-center justify-center gap-2"
        >
          {sending ? (
            <>
              <Loader2 className="w-4 h-4 animate-spin" />
              Sending...
            </>
          ) : (
            'Send Broadcast'
          )}
        </button>
      </div>
    </div>
  );
}
