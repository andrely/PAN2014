import logging
import sys

if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)

    if len(sys.argv) != 2:
        print 'Usage: %s <score file>' % sys.argv[0]
        sys.exit(1)

    with open(sys.argv[1]) as f:
        score_lines = [line.strip().split('\t') for line in f]

    no_plag = [m for m in score_lines if m[2] == 'no-plagiarism']
    no_obf = [m for m in score_lines if m[2] == 'no-obfuscation']
    rand = [m for m in score_lines if m[2] == 'random-obfuscation']
    trans = [m for m in score_lines if m[2] == 'translation-obfuscation']
    summary = [m for m in score_lines if m[2] == 'summary-obfuscation']

    if not len(no_plag) == (len(no_obf) + len(rand) + len(trans) + len(summary)):
        logging.warn('Inconsistent number of entries')
        sys.exit(1)

    no_plag_idx = 0

    for cat, entries in zip(['no-obfuscation', 'random-obfuscation', 'translation-obfuscation', 'summary-obfuscation'],
                            [no_obf, rand, trans, summary]):
        n = len(entries)

        train_len = int(0.8*n)
        held_len = int(0.1*n)
        test_len = n - train_len - held_len

        train_entries = entries[0:train_len] + no_plag[no_plag_idx:no_plag_idx+train_len]
        no_plag_idx += train_len
        held_entries = entries[train_len: train_len + held_len] + no_plag[no_plag_idx:no_plag_idx+held_len]
        no_plag_idx += held_len
        test_entries = entries[train_len + held_len:] + no_plag[no_plag_idx:no_plag_idx+test_len]
        no_plag_idx += test_len

        with open('pairs-%s' % cat, 'w') as f:
            for e in entries:
                f.write("%s %s\n" % (e[0], e[1]))

        with open('%s-train.tsv' % cat, 'w') as f:
            for e in train_entries:
                f.write('\t'.join(e) + '\n')

        with open('%s-held.tsv' % cat, 'w') as f:
            for e in held_entries:
                f.write('\t'.join(e) + '\n')

        with open('%s-test.tsv' % cat, 'w') as f:
            for e in test_entries:
                f.write('\t'.join(e) + '\n')
