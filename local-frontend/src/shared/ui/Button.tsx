import type { ButtonHTMLAttributes } from 'react'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '../utils/cn'

/* padding은 arbitrary px 사용 — :root font가 15px라 rem 기반 스텝(px-3.5 등)은
   App.css 원본 픽셀값(14/12/10px…)과 일치하지 않는다 */
const buttonVariants = cva(
  'cursor-pointer rounded-sm border-0 font-bold disabled:cursor-not-allowed',
  {
    variants: {
      variant: {
        primary: 'bg-accent-strong px-[14px] py-[10px] text-accent-ink',
        ghost:
          'bg-transparent px-[12px] py-[9px] text-text-muted hover:bg-surface hover:text-text',
      },
      size: {
        default: '',
        compact: 'px-[10px] py-[8px] text-[13px]',
      },
    },
    defaultVariants: {
      variant: 'primary',
      size: 'default',
    },
  },
)

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> &
  VariantProps<typeof buttonVariants>

export function Button({ className, variant, size, ...props }: ButtonProps) {
  return (
    <button
      className={cn(buttonVariants({ variant, size }), className)}
      {...props}
    />
  )
}
