import { useFormStatus } from 'react-dom';
import { UserPlus } from 'lucide-react';

/**
 * Submit button that leverages React 19's `useFormStatus` hook to
 * automatically reflect the pending state of the parent `<form action>`.
 */
export default function SubmitButton({ disabled }: { disabled?: boolean }) {
  const { pending } = useFormStatus();

  return (
    <button
      type="submit"
      disabled={pending || disabled}
      className="btn-primary flex-1"
    >
      {pending ? (
        <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
      ) : (
        <>
          <UserPlus className="w-5 h-5" />
          Create account
        </>
      )}
    </button>
  );
}
