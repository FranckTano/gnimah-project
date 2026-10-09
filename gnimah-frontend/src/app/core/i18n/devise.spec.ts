import { CurrencyPipe } from '@angular/common';
import { DEFAULT_CURRENCY_CODE, LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { CODE_DEVISE, LOCALE_APP, formatMontant } from './devise';

/** Les espaces insécables du français (U+00A0, U+202F) ramenés à des espaces simples. */
const lisible = (texte: string | null): string => (texte ?? '').replace(/\s/g, ' ');

describe('Affichage des montants en franc CFA', () => {
  let pipe: CurrencyPipe;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CurrencyPipe,
        { provide: LOCALE_ID, useValue: LOCALE_APP },
        { provide: DEFAULT_CURRENCY_CODE, useValue: CODE_DEVISE }
      ]
    });
    pipe = TestBed.inject(CurrencyPipe);
  });

  it('le pipe currency affiche « FCFA », sans centimes ni code ISO', () => {
    const texte = pipe.transform(15000);
    expect(lisible(texte)).toBe('15 000 FCFA');
    expect(texte).not.toContain('XOF');
  });

  it('formatMontant rend exactement la même chose que le pipe', () => {
    expect(formatMontant(1250000)).toBe(pipe.transform(1250000)!);
    expect(lisible(formatMontant(1250000))).toBe('1 250 000 FCFA');
  });

  it('une valeur absente s\'affiche « 0 FCFA »', () => {
    expect(lisible(formatMontant(null))).toBe('0 FCFA');
    expect(lisible(formatMontant(undefined))).toBe('0 FCFA');
  });

  it('un écart négatif garde son signe', () => {
    expect(lisible(formatMontant(-5000))).toBe('-5 000 FCFA');
  });

  it('arrondit au franc près, la devise n\'ayant pas de centimes', () => {
    expect(lisible(formatMontant(9999.6))).toBe('10 000 FCFA');
  });
});
