import os
import re

from lxml import etree


def parse_truth_fn(fn):
    base_fn, _ = os.path.splitext(os.path.basename(fn))
    parts = base_fn.split('-')

    if not len(parts) == 4 and parts[0] == 'suspicious' and parts[2] == 'source':
        raise ValueError

    return '-'.join(parts[0:2]) + '.txt', '-'.join(parts[2:4]) + '.txt'


def parse_truth_file(fn):
    doc = etree.parse(fn)
    document = doc.getroot()

    reference = document.attrib['reference']

    features = [feat.attrib for feat in document.findall('feature') if feat.attrib.get('name') == 'plagiarism']

    return features


def get_sents(filename, offset, length, collection):
    sents = [sent['id'] for sent in collection.find({'filename': filename,
                                                     'offset': {'$gte': offset, '$lte': offset + length}})]

    return sents


def align_sents(susp_sents, src_sents, db):
    if len(susp_sents) == len(src_sents):
        return zip(susp_sents, src_sents)

    src_coll = db['source_sentences']
    susp_coll = db['suspicious_sentences']

    src_lengths = [src_coll.find_one({'id': sent_id})['length'] for sent_id in src_sents]
    susp_lengths = [susp_coll.find_one({'id': sent_id})['length'] for sent_id in susp_sents]

    if len(src_lengths) > len(susp_lengths):
        diff = len(src_lengths) - len(susp_lengths)

        if abs(sum(src_lengths[diff:]) - sum(susp_lengths)) > abs(sum(src_lengths[0:-diff]) - sum(susp_lengths)):
            return zip(susp_sents, src_sents[diff:])
        else:
            return zip(susp_sents, src_sents[0:-diff])
    else:
        diff = len(susp_lengths) - len(src_lengths)

        if abs(sum(susp_lengths[diff:]) - sum(src_lengths)) > abs(sum(susp_lengths[0:-diff]) - sum(src_lengths)):
            return zip(susp_sents[diff:], src_sents)
        else:
            return zip(susp_sents[0:-diff], src_sents)


def get_plagiarism_sents(susp_fn, feature, db):
    src_coll = db['source_sentences']
    susp_coll = db['suspicious_sentences']

    susp_sents = get_sents(os.path.basename(susp_fn), int(feature['this_offset']),
                           int(feature['this_length']), susp_coll)
    src_sents = get_sents(feature['source_reference'], int(feature['source_offset']),
                          int(feature['source_length']), src_coll)

    return align_sents(susp_sents, src_sents, db)


def get_plagiarism(truth_fn, db):
    susp_fn, src_fn = parse_truth_fn(truth_fn)

    features = parse_truth_file(truth_fn)

    if not features:
        return None
    else:
        return [(susp_fn, src_fn, get_plagiarism_sents(susp_fn, feat, db)) for feat in features]


def parse_subcorpora_dir(dir):
    m = re.search("^\d\d-(.+)", dir)

    if m:
        return m.group(1)
    else:
        return None
