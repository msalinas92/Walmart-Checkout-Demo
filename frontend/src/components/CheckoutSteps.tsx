import Link from "next/link";

const steps = [
  { number: 1, label: "Productos", href: "/" },
  { number: 2, label: "Carrito", href: "/carrito" },
  { number: 3, label: "Pago", href: "/checkout" }
];

export function CheckoutSteps({ current }: { current: number }) {
  return <nav className="checkout-steps" aria-label="Progreso de compra">
    {steps.map((step) => <Link key={step.number} href={step.href} className={step.number === current ? "current" : step.number < current ? "complete" : ""}>
      <span>{step.number < current ? "✓" : step.number}</span><b>{step.label}</b>
    </Link>)}
  </nav>;
}
